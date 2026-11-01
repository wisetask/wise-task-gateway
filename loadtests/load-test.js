import http from "k6/http";
import { sleep } from "k6";
import { Counter, Trend } from "k6/metrics";
import { loadConfig, buildStages } from "./config.js";
import { OPS } from "./operations.js";

const CONFIG = loadConfig(__ENV);

function sanitizeMetricName(name) {
  return name.replace(/[^A-Za-z0-9_]/g, "_");
}

const DURATION_METRICS = {};
const OK_METRICS = {};
const FAIL_METRICS = {};
const EXPECTED_METRICS = {};
for (const op of OPS) {
  const safe = sanitizeMetricName(op.name);
  DURATION_METRICS[op.name] = new Trend(`op_duration_${safe}`, true);
  OK_METRICS[op.name] = new Counter(`op_ok_${safe}`);
  FAIL_METRICS[op.name] = new Counter(`op_fail_${safe}`);
  EXPECTED_METRICS[op.name] = new Counter(`op_expected_${safe}`);
}
const TOTAL_DURATION = new Trend("op_duration_total", true);

export const options = {
  scenarios: {
    load: {
      executor: "ramping-vus",
      startVUs: CONFIG.rampUp > 0 ? 0 : CONFIG.vus,
      stages: buildStages(CONFIG)
    }
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(50)", "p(90)", "p(95)", "p(99)"],
  thresholds: {
    "http_req_failed": ["rate<0.5"]
  },
  insecureSkipTLSVerify: true,
  userAgent: "wise-task-loadtest-k6/1.0",
  setupTimeout: "30s",
  teardownTimeout: "30s",
  noConnectionReuse: false
};

let vuAuthToken = null;
let vuRefreshToken = null;

function graphqlRequest(ctx, query, variables, opName, group) {
  const headers = { "content-type": "application/json" };
  const token = vuAuthToken || ctx.token;
  if (ctx.opNeedsAuth && !ctx.opNoAuthHeader && token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return http.post(`${CONFIG.url}/graphql`, JSON.stringify({ query, variables }), {
    tags: { name: opName, group },
    timeout: CONFIG.timeout,
    headers
  });
}

function classifyGraphql(res, expected) {
  if (res.status !== 200) return "fail";
  let body;
  try {
    body = JSON.parse(res.body);
  } catch {
    return "fail";
  }
  if (body.errors && body.errors.length > 0) {
    return expected ? "expected" : "fail";
  }
  if (!body.data) return "fail";
  return "ok";
}

function classifyRest(res, op) {
  if (res.status !== 200) return "fail";
  if (op.validate && op.validate(res.body)) return "fail";
  return "ok";
}

function recordOutcome(op, outcome) {
  if (outcome === "ok") OK_METRICS[op.name].add(1);
  else if (outcome === "fail") FAIL_METRICS[op.name].add(1);
  else EXPECTED_METRICS[op.name].add(1);
}

function reauthenticate(email, password) {
  if (!email || !password) return false;
  const res = graphqlRequest(
    { opNeedsAuth: false },
    `mutation SignIn($req: SignInRequest!) {
  signIn(signInRequest: $req) { accessToken refreshToken }
}`,
    { req: { email, password } },
    "signIn-reauth",
    "auth"
  );
  if (classifyGraphql(res, false) !== "ok") return false;
  try {
    const body = JSON.parse(res.body);
    if (!body.data.signIn.accessToken) return false;
    vuAuthToken = body.data.signIn.accessToken;
    vuRefreshToken = body.data.signIn.refreshToken;
    return true;
  } catch {
    return false;
  }
}

function executeGraphqlOp(op, ctx) {
  const vars = op.variables ? op.variables(ctx) : {};
  const execCtx = {
    ...ctx,
    opNeedsAuth: op.needsAuth,
    opNoAuthHeader: op.noAuthHeader
  };
  const res = graphqlRequest(execCtx, op.query, vars, op.name, op.group);
  DURATION_METRICS[op.name].add(res.timings.duration);
  TOTAL_DURATION.add(res.timings.duration);
  let outcome = classifyGraphql(res, op.expectErrors ? op.expectErrors(ctx, vars) : false);
  if (outcome === "fail" && res.status === 401 && op.needsAuth && !op.noAuthHeader && CONFIG.email) {
    if (reauthenticate(CONFIG.email, CONFIG.password)) {
      const retry = graphqlRequest(execCtx, op.query, vars, op.name, op.group);
      DURATION_METRICS[op.name].add(retry.timings.duration);
      TOTAL_DURATION.add(retry.timings.duration);
      outcome = classifyGraphql(retry, op.expectErrors ? op.expectErrors(ctx, vars) : false);
    }
  }
  recordOutcome(op, outcome);
}

function executeCompositeOp(op, ctx) {
  const createVars = op.createVariables(ctx);
  const execCtx = { ...ctx, opNeedsAuth: op.needsAuth, opNoAuthHeader: op.noAuthHeader };
  const createRes = graphqlRequest(execCtx, op.createQuery, createVars, "createGraph", op.group);
  const createOutcome = classifyGraphql(createRes, false);
  if (createOutcome !== "ok") {
    DURATION_METRICS[op.name].add(createRes.timings.duration);
    TOTAL_DURATION.add(createRes.timings.duration);
    recordOutcome(op, createOutcome);
    return;
  }
  let graphId = null;
  try {
    graphId = JSON.parse(createRes.body).data.createGraph.id;
  } catch {
    graphId = null;
  }
  if (!graphId) {
    DURATION_METRICS[op.name].add(createRes.timings.duration);
    TOTAL_DURATION.add(createRes.timings.duration);
    recordOutcome(op, "fail");
    return;
  }
  const deleteRes = graphqlRequest(execCtx, op.deleteQuery, { id: graphId }, "deleteGraph", op.group);
  DURATION_METRICS[op.name].add(createRes.timings.duration + deleteRes.timings.duration);
  TOTAL_DURATION.add(createRes.timings.duration + deleteRes.timings.duration);
  recordOutcome(op, classifyGraphql(deleteRes, false));
}

function executeRestOp(op) {
  const res = http.get(`${CONFIG.url}${op.path}`, {
    tags: { name: op.name, group: op.group },
    timeout: CONFIG.timeout
  });
  DURATION_METRICS[op.name].add(res.timings.duration);
  TOTAL_DURATION.add(res.timings.duration);
  recordOutcome(op, classifyRest(res, op));
}

function activeOps(ctx) {
  const active = [];
  for (const op of OPS) {
    if (op.enabled && !op.enabled(ctx)) continue;
    if (op.needsAuth && !(ctx.authenticated || vuAuthToken)) continue;
    active.push(op);
  }
  return active;
}

function pickOp(ctx, candidates) {
  let totalWeight = 0;
  const weighted = [];
  for (const op of candidates) {
    const weight = op.weightKey ? CONFIG[op.weightKey] : (op.weight || 0);
    if (weight <= 0) continue;
    weighted.push([op, weight]);
    totalWeight += weight;
  }
  if (totalWeight === 0) return null;
  let roll = Math.random() * totalWeight;
  for (const [op, weight] of weighted) {
    roll -= weight;
    if (roll <= 0) return op;
  }
  return weighted[weighted.length - 1][0];
}

export function setup() {
  let appVersion = "unknown";
  try {
    const info = http.get(`${CONFIG.url}/actuator/info`, { timeout: "10s" });
    appVersion = (info.json("build.version") || info.json("version")) || "unknown";
  } catch {
    appVersion = "unknown";
  }

  let authenticated = false;
  let token = null;
  let refreshToken = null;
  let userId = null;
  if (CONFIG.email && CONFIG.password) {
    const res = graphqlRequest(
      { opNeedsAuth: false },
      `mutation SignIn($req: SignInRequest!) {
  signIn(signInRequest: $req) { accessToken refreshToken }
}`,
      { req: { email: CONFIG.email, password: CONFIG.password } },
      "signIn-setup",
      "auth"
    );
    const outcome = classifyGraphql(res, false);
    if (outcome === "ok") {
      const body = res.json();
      token = body.data.signIn.accessToken;
      refreshToken = body.data.signIn.refreshToken;
      authenticated = true;
      try {
        let b64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
        while (b64.length % 4 !== 0) b64 += "=";
        userId = JSON.parse(atob(b64)).sub || null;
      } catch {
        userId = null;
      }
      console.log(`authenticated as ${CONFIG.email} (userId: ${userId || "unknown"})`);
    } else {
      console.log(`signIn failed (HTTP ${res.status}): authenticated operations will be excluded`);
    }
  } else {
    console.log("no credentials configured: authenticated operations will be excluded");
  }

  const sample = { taskId: null, graphId: null, pluginId: null };
  if (authenticated) {
    const samples = [
      ["getAllTasks", "taskId"],
      ["getAllGraphs", "graphId"],
      ["getAllPlugins", "pluginId"]
    ];
    for (const [field, key] of samples) {
      try {
        const res = http.post(
          `${CONFIG.url}/graphql`,
          JSON.stringify({
            query: `query Sample($req: GetAll${field === "getAllTasks" ? "Task" : field === "getAllGraphs" ? "Graphs" : "Plugin"}RequestInput!) {
  ${field}(request: $req) { items { id } }
}`,
            variables: { req: { pagination: { page: 0, pageSize: 1 }, filter: {} } }
          }),
          { headers: { "content-type": "application/json", Authorization: `Bearer ${token}` }, timeout: CONFIG.timeout }
        );
        const body = res.json();
        sample[key] = (body.data && body.data[field] && body.data[field].items && body.data[field].items[0] && body.data[field].items[0].id) || null;
        console.log(`sample ${key}: ${sample[key] || "none found"}`);
      } catch (error) {
        console.log(`sample ${key} fetch failed: ${error}`);
      }
    }
  }

  return {
    token,
    refreshToken,
    userId,
    sample,
    authenticated,
    appVersion
  };
}

export default function (data) {
  const ctx = {
    config: CONFIG,
    token: data.token,
    refreshToken: vuRefreshToken || data.refreshToken,
    userId: data.userId,
    sample: data.sample,
    authenticated: data.authenticated
  };
  const op = pickOp(ctx, activeOps(ctx));
  if (!op) {
    sleep(0.1);
    return;
  }
  if (op.isRest) {
    executeRestOp(op);
  } else if (op.isComposite) {
    executeCompositeOp(op, ctx);
  } else {
    executeGraphqlOp(op, ctx);
  }
  if (CONFIG.thinkTime > 0) {
    sleep(CONFIG.thinkTime + (CONFIG.thinkTimeJitter > 0 ? Math.random() * CONFIG.thinkTimeJitter : 0));
  }
}

function formatMs(value) {
  if (value >= 100) return value.toFixed(0);
  if (value >= 10) return value.toFixed(1);
  return value.toFixed(2);
}

function buildResult(data) {
  const windowMs = data.state.testRunDurationMs || 0;
  const operations = {};
  let totalRequests = 0;
  let totalOk = 0;
  let totalFail = 0;
  let totalExpected = 0;
  for (const op of OPS) {
    const trend = data.metrics[`op_duration_${sanitizeMetricName(op.name)}`];
    const ok = data.metrics[`op_ok_${sanitizeMetricName(op.name)}`];
    const fail = data.metrics[`op_fail_${sanitizeMetricName(op.name)}`];
    const expected = data.metrics[`op_expected_${sanitizeMetricName(op.name)}`];
    const okCount = ok ? ok.values.count : 0;
    const failCount = fail ? fail.values.count : 0;
    const expectedCount = expected ? expected.values.count : 0;
    const requests = okCount + failCount + expectedCount;
    const values = trend ? trend.values : {};
    operations[op.name] = {
      group: op.group,
      requests,
      ok: okCount,
      fail: failCount,
      expected: expectedCount,
      rps: windowMs > 0 ? Number((requests / (windowMs / 1000)).toFixed(2)) : 0,
      mean: Number((values.avg || 0).toFixed(2)),
      min: Number((values.min || 0).toFixed(2)),
      p50: Number((values["p(50)"] || 0).toFixed(2)),
      p90: Number((values["p(90)"] || 0).toFixed(2)),
      p95: Number((values["p(95)"] || 0).toFixed(2)),
      p99: Number((values["p(99)"] || 0).toFixed(2)),
      max: Number((values.max || 0).toFixed(2)),
      statuses: {},
      errors: {}
    };
    totalRequests += requests;
    totalOk += okCount;
    totalFail += failCount;
    totalExpected += expectedCount;
  }
  const dataReceived = data.metrics.data_received ? data.metrics.data_received.values.count : 0;
  const totalValues = data.metrics.op_duration_total ? data.metrics.op_duration_total.values : {};
  return {
    meta: {
      target: CONFIG.url,
      appVersion: (data.setup_data && data.setup_data.appVersion) || "unknown",
      label: CONFIG.label,
      hostname: null,
      node: null,
      authenticated: data.setup_data ? data.setup_data.authenticated : false,
      startedAt: new Date(Date.now() - windowMs).toISOString(),
      finishedAt: new Date().toISOString(),
      windowMs: Math.round(windowMs),
      config: {
        vus: CONFIG.vus,
        durationMs: CONFIG.duration,
        rampUpMs: CONFIG.rampUp,
        rampDownMs: CONFIG.rampDown,
        warmupMs: 0,
        timeoutMs: CONFIG.timeout,
        thinkTimeMs: CONFIG.thinkTime * 1000,
        signUpEnabled: CONFIG.signUpEnabled,
        writeEnabled: CONFIG.writeEnabled,
        solveTaskId: CONFIG.solveTaskId,
        graphVertexCount: CONFIG.graphVertexCount,
        graphEdgeCount: CONFIG.graphEdgeCount,
        signInEmail: CONFIG.email ? "configured" : "not-configured"
      }
    },
    totals: {
      requests: totalRequests,
      ok: totalOk,
      fail: totalFail,
      expected: totalExpected,
      rps: windowMs > 0 ? Number((totalRequests / (windowMs / 1000)).toFixed(2)) : 0,
      mean: Number((totalValues.avg || 0).toFixed(2)),
      p50: Number((totalValues["p(50)"] || 0).toFixed(2)),
      p90: Number((totalValues["p(90)"] || 0).toFixed(2)),
      p95: Number((totalValues["p(95)"] || 0).toFixed(2)),
      p99: Number((totalValues["p(99)"] || 0).toFixed(2)),
      max: Number((totalValues.max || 0).toFixed(2)),
      maxInFlight: null,
      bytes: dataReceived
    },
    operations,
    timeline: []
  };
}

function renderReport(result) {
  const meta = result.meta;
  const totals = result.totals;
  const lines = [];
  lines.push(`target: ${meta.target}  version: ${meta.appVersion}${meta.label ? `  label: ${meta.label}` : ""}`);
  lines.push(`window: ${(meta.windowMs / 1000).toFixed(0)}s  vus: ${meta.config.vus}  auth: ${meta.authenticated ? "yes" : "no"}`);
  lines.push("");
  lines.push(
    "operation".padEnd(28) +
      "reqs".padStart(9) + "  " +
      "ok".padStart(9) + "  " +
      "fail".padStart(6) + "  " +
      "exp".padStart(6) + "  " +
      "rps".padStart(8) + "  " +
      "p50".padStart(8) + "  " +
      "p90".padStart(8) + "  " +
      "p95".padStart(8) + "  " +
      "p99".padStart(8) + "  " +
      "max".padStart(8)
  );
  const row = (name, s) =>
    name.padEnd(28) +
    String(s.requests).padStart(9) + "  " +
    String(s.ok).padStart(9) + "  " +
    String(s.fail).padStart(6) + "  " +
    String(s.expected).padStart(6) + "  " +
    s.rps.toFixed(1).padStart(8) + "  " +
    formatMs(s.p50).padStart(8) + "  " +
    formatMs(s.p90).padStart(8) + "  " +
    formatMs(s.p95).padStart(8) + "  " +
    formatMs(s.p99).padStart(8) + "  " +
    formatMs(s.max).padStart(8);
  for (const [name, summary] of Object.entries(result.operations)) {
    if (summary.requests === 0) continue;
    lines.push(row(name, summary));
  }
  lines.push("-".repeat(120));
  lines.push(row("TOTAL", totals));
  lines.push("");
  lines.push(`transferred: ${(totals.bytes / 1048576).toFixed(2)} MB`);
  const failing = Object.entries(result.operations).filter(([, s]) => s.fail > 0);
  if (failing.length > 0) {
    lines.push("");
    lines.push(`operations with failures: ${failing.map(([name, s]) => `${name} (${s.fail})`).join(", ")}`);
  }
  return lines.join("\n");
}

export function handleSummary(data) {
  const result = buildResult(data);
  const files = {
    stdout: renderReport(result) + "\n"
  };
  if (CONFIG.out) {
    files[CONFIG.out] = JSON.stringify(result, null, 2) + "\n";
  }
  return files;
}

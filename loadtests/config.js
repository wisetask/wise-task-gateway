function parseDurationMs(raw, fallbackMs) {
  if (raw === undefined || raw === null || raw === "") return fallbackMs;
  const match = String(raw).trim().match(/^(\d+(?:\.\d+)?)\s*(ms|s|m|h)?$/);
  if (!match) throw new Error(`Invalid duration: ${raw}`);
  const value = Number(match[1]);
  switch (match[2]) {
    case "ms": return value;
    case "s": return value * 1000;
    case "m": return value * 60000;
    case "h": return value * 3600000;
    default: return value * 1000;
  }
}

function toDurationString(ms) {
  return `${Math.round(ms / 1000)}s`;
}

function bool(raw, fallback) {
  if (raw === undefined || raw === "") return fallback;
  return raw === "1" || String(raw).toLowerCase() === "true" || raw === "yes";
}

export function loadConfig(env) {
  const config = {
    url: (env.GATEWAY_URL || "http://localhost:8084").replace(/\/+$/, ""),
    vus: Number(env.LOADTEST_VUS || 10),
    duration: parseDurationMs(env.LOADTEST_DURATION, 60000),
    rampUp: parseDurationMs(env.LOADTEST_RAMP_UP, 15000),
    rampDown: parseDurationMs(env.LOADTEST_RAMP_DOWN, 10000),
    timeout: env.LOADTEST_TIMEOUT || "30s",
    thinkTime: parseDurationMs(env.LOADTEST_THINK_TIME, 0) / 1000,
    thinkTimeJitter: parseDurationMs(env.LOADTEST_THINK_TIME_JITTER, 0) / 1000,
    email: env.LOADTEST_SIGN_IN_EMAIL || null,
    password: env.LOADTEST_SIGN_IN_PASSWORD || null,
    solveTaskId: env.LOADTEST_SOLVE_TASK_ID || null,
    signUpEnabled: bool(env.LOADTEST_SIGN_UP, false),
    writeEnabled: bool(env.LOADTEST_WRITES, false),
    graphVertexCount: Number(env.LOADTEST_GRAPH_VERTICES || 20),
    graphEdgeCount: Number(env.LOADTEST_GRAPH_EDGES || 20),
    unauthorizedWeight: Number(env.LOADTEST_UNAUTHORIZED_WEIGHT || 3),
    healthWeight: Number(env.LOADTEST_HEALTH_WEIGHT || 5),
    prometheusWeight: Number(env.LOADTEST_PROMETHEUS_WEIGHT || 2),
    label: env.LOADTEST_LABEL || null,
    out: env.LOADTEST_OUT || ""
  };
  if (env.LOADTEST_NO_RAMP === "1") {
    config.rampUp = 0;
    config.rampDown = 0;
  }
  return config;
}

export function buildStages(config) {
  const stages = [];
  if (config.rampUp > 0) stages.push({ duration: toDurationString(config.rampUp), target: config.vus });
  stages.push({ duration: toDurationString(config.duration), target: config.vus });
  if (config.rampDown > 0) stages.push({ duration: toDurationString(config.rampDown), target: 0 });
  return stages;
}

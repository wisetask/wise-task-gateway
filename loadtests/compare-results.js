import fs from "node:fs";
import path from "node:path";

function failRate(operation) {
  if (!operation || operation.requests === 0) return 0;
  return (operation.fail / operation.requests) * 100;
}

function deltaPercent(before, after) {
  if (!Number.isFinite(before) || !Number.isFinite(after) || before === 0) return null;
  return ((after - before) / before) * 100;
}

function formatDelta(percent, digits = 1) {
  if (percent === null || !Number.isFinite(percent)) return "n/a";
  const sign = percent > 0 ? "+" : "";
  return `${sign}${percent.toFixed(digits)}%`;
}

function collectOperationNames(a, b) {
  const names = new Set([...Object.keys(a.operations), ...Object.keys(b.operations)]);
  const known = new Set(["signIn", "signUp", "refreshToken", "getMyProfile", "getAllProfiles", "getAllTasks", "getTask", "getAllTaskSolutions", "getAllGraphs", "getGraphById", "getAllPlugins", "getPlugin", "getStatistic-sum-user", "getStatistic-success-task", "generateGraph", "createGraph", "deleteGraph", "solveTaskGraph", "getMyProfile-unauthorized", "actuator-health", "actuator-prometheus"]);
  return [...names].sort((x, y) => {
    const ix = known.has(x) ? [...known].indexOf(x) : 999;
    const iy = known.has(y) ? [...known].indexOf(y) : 999;
    return ix - iy || x.localeCompare(y);
  });
}

export function compare(before, after, thresholdPercent) {
  const regressions = [];
  const lines = [];
  const metaLine = (result) =>
    `${result.meta.appVersion}${result.meta.label ? ` (${result.meta.label})` : ""} @ ${result.meta.target}, ${(result.meta.windowMs / 1000).toFixed(0)}s, ${result.meta.config.vus} vus`;

  lines.push(`before: ${metaLine(before)}`);
  lines.push(`after:  ${metaLine(after)}`);
  lines.push("");
  lines.push(
    "operation".padEnd(28) +
      "rps".padStart(20) + "  " +
      "p95".padStart(20) + "  " +
      "fail%".padStart(16)
  );
  lines.push("-".repeat(90));

  const row = (name, opA, opB) => {
    const rpsDelta = deltaPercent(opA?.rps, opB?.rps);
    const p95Delta = deltaPercent(opA?.p95, opB?.p95);
    const failA = failRate(opA);
    const failB = failRate(opB);
    const rpsText = `${(opA?.rps ?? 0).toFixed(1)} → ${(opB?.rps ?? 0).toFixed(1)} (${formatDelta(rpsDelta)})`;
    const p95Text = `${opA?.p95 ?? 0} → ${opB?.p95 ?? 0} ms (${formatDelta(p95Delta)})`;
    const failText = `${failA.toFixed(2)}% → ${failB.toFixed(2)}%`;
    let flag = "";
    if (rpsDelta !== null && rpsDelta < -thresholdPercent) {
      flag = " REGRESSION";
      regressions.push(`${name}: rps ${formatDelta(rpsDelta)}`);
    }
    if (p95Delta !== null && p95Delta > thresholdPercent) {
      flag = " REGRESSION";
      regressions.push(`${name}: p95 ${formatDelta(p95Delta)}`);
    }
    if (failB - failA > 1) {
      flag = " REGRESSION";
      regressions.push(`${name}: fail rate ${failA.toFixed(2)}% → ${failB.toFixed(2)}%`);
    }
    lines.push(name.padEnd(28) + rpsText.padStart(20) + "  " + p95Text.padStart(20) + "  " + failText.padStart(16) + flag);
  };

  for (const name of collectOperationNames(before, after)) {
    row(name, before.operations[name], after.operations[name]);
  }
  lines.push("-".repeat(90));
  row("TOTAL", { rps: before.totals.rps, p95: before.totals.p95, requests: before.totals.requests, fail: before.totals.fail }, { rps: after.totals.rps, p95: after.totals.p95, requests: after.totals.requests, fail: after.totals.fail });
  lines.push("");
  const impA = before.totals, impB = after.totals;
  lines.push(`requests: ${impA.requests} → ${impB.requests}   in-flight peak: ${impA.maxInFlight} → ${impB.maxInFlight}`);
  if (regressions.length === 0) {
    lines.push(`no regressions above ${thresholdPercent}% threshold`);
  } else {
    lines.push(`regressions above ${thresholdPercent}% threshold: ${regressions.length}`);
    for (const regression of regressions) lines.push(`  ${regression}`);
  }
  return { lines: lines.join("\n"), regressions };
}

function loadResult(file) {
  const resolved = path.resolve(file);
  const raw = JSON.parse(fs.readFileSync(resolved, "utf8"));
  if (!raw.totals || !raw.operations) {
    throw new Error(`${resolved} is not a load test result file`);
  }
  return raw;
}

function main() {
  const args = process.argv.slice(2);
  const files = args.filter((arg) => !arg.startsWith("--"));
  const thresholdArg = args.find((arg) => arg.startsWith("--threshold="));
  const threshold = thresholdArg ? Number(thresholdArg.split("=")[1]) : 15;
  const noFail = args.includes("--no-fail");
  if (files.length !== 2) {
    process.stdout.write("usage: node compare-results.js <before.json> <after.json> [--threshold=15] [--no-fail]\n");
    process.exitCode = 1;
    return;
  }
  try {
    const before = loadResult(files[0]);
    const after = loadResult(files[1]);
    const { lines, regressions } = compare(before, after, threshold);
    process.stdout.write(lines + "\n");
    if (regressions.length > 0 && !noFail) process.exitCode = 1;
  } catch (error) {
    process.stdout.write(`compare failed: ${error.message}\n`);
    process.exitCode = 1;
  }
}

if (process.argv[1] && process.argv[1].endsWith("compare-results.js")) {
  main();
}

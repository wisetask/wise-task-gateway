import http from "node:http";

function encodeJwt(claims) {
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(JSON.stringify(claims)).toString("base64url");
  return `${header}.${payload}.mock-signature`;
}

function tokenPair() {
  const now = Math.floor(Date.now() / 1000);
  return {
    accessToken: encodeJwt({ sub: "mock-profile-1", iss: "wise-task", type: "access", exp: now + 3600 }),
    refreshToken: encodeJwt({ sub: "mock-profile-1", iss: "wise-task", type: "refresh", exp: now + 604800 })
  };
}

function pagination(totalCount) {
  return { page: 0, pageSize: 20, totalCount, totalPages: Math.ceil(totalCount / 20) || 1, hasNext: false, hasPrevious: false };
}

const RESOLVERS = {
  signIn: () => tokenPair(),
  signUp: () => tokenPair(),
  refreshToken: () => tokenPair(),
  getMyProfile: () => ({ id: "mock-profile-1", email: "loadtest@example.com", firstName: "Load", lastName: "Test", patronymic: null, profileRole: "USER" }),
  getAllProfiles: () => ({ items: [RESOLVERS.getMyProfile()], pagination: pagination(1) }),
  getAllTasks: () => ({
    items: [{ id: "mock-task-1", name: "Mock task", category: "loadtest", taskType: "GRAPH", isPublic: true }],
    pagination: pagination(1)
  }),
  getTask: () => ({
    id: "mock-task-1",
    name: "Mock task",
    description: "Mock description",
    category: "loadtest",
    taskType: "GRAPH",
    isPublic: true,
    isHiddenMistake: false,
    rule: { isColor: true, isEdit: true, isMove: true, isDelete: false }
  }),
  getAllTaskSolutions: () => ({ items: [{ id: "mock-solution-1", isCorrect: true }], pagination: pagination(1) }),
  getAllGraphs: () => ({
    items: [{ id: "mock-graph-1", vertexCount: 10, edgeCount: 10, isDirect: false }],
    pagination: pagination(1)
  }),
  getGraphById: () => ({ id: "mock-graph-1", vertexCount: 10, edgeCount: 10, isDirect: false, isNamed: false, name: "Mock graph" }),
  getAllPlugins: () => ({
    items: [{ id: "mock-plugin-1", name: "Mock plugin", category: "loadtest", graphType: "ANY", isValid: true, pluginType: "GRAPH_PROPERTY", isInternal: false }],
    pagination: pagination(1)
  }),
  getPlugin: () => ({ id: "mock-plugin-1", name: "Mock plugin", description: "Mock description", category: "loadtest", graphType: "ANY", isValid: true, pluginType: "GRAPH_PROPERTY", isInternal: false }),
  getStatistic: () => ({ scope: "USER", type: "SUM", value: 42.0, updated_at: new Date().toISOString() }),
  generateGraph: (variables) => {
    const vertexCount = variables?.req?.vertexCount ?? 10;
    return { id: "mock-generated-1", vertexCount, edgeCount: variables?.req?.edgeCount ?? 10, isDirect: variables?.req?.isDirect ?? false };
  },
  createGraph: (variables) => ({ id: `mock-created-${Date.now()}`, vertexCount: variables?.graph?.vertexCount ?? 10 }),
  deleteGraph: () => "mock-graph-deleted",
  solveTaskGraph: () => ({ id: "mock-solution-1", isCorrect: true })
};

const PUBLIC_RESOLVERS = new Set(["signIn", "signUp", "refreshToken"]);

function detectResolver(query) {
  const match = query.match(/(?:query|mutation)[^{]*\{\s*(\w+)/);
  if (!match) return null;
  return match[1];
}

function handleGraphql(req, res, bodyText) {
  let body;
  try {
    body = JSON.parse(bodyText);
  } catch {
    res.writeHead(400);
    res.end(JSON.stringify({ errors: [{ message: "invalid JSON" }] }));
    return;
  }
  const resolver = detectResolver(body.query || "");
  const canned = resolver ? RESOLVERS[resolver] : null;
  if (!canned) {
    res.writeHead(200, { "content-type": "application/json" });
    res.end(JSON.stringify({ errors: [{ message: `unknown operation: ${resolver ?? "none"}` }] }));
    return;
  }
  const needsAuth = !PUBLIC_RESOLVERS.has(resolver);
  if (needsAuth && !req.headers.authorization) {
    setTimeout(() => {
      res.writeHead(200, { "content-type": "application/json" });
      res.end(JSON.stringify({ errors: [{ message: "Access Denied" }] }));
    }, 1 + Math.floor(Math.random() * 4));
    return;
  }
  const data = { [resolver]: canned(body.variables) };
  setTimeout(() => {
    res.writeHead(200, { "content-type": "application/json" });
    res.end(JSON.stringify({ data }));
  }, 1 + Math.floor(Math.random() * 4));
}

export function startMockServer(port) {
  const server = http.createServer((req, res) => {
    if (req.method === "GET" && req.url === "/actuator/health") {
      res.writeHead(200, { "content-type": "application/json" });
      res.end(JSON.stringify({ status: "UP" }));
      return;
    }
    if (req.method === "GET" && req.url === "/actuator/info") {
      res.writeHead(200, { "content-type": "application/json" });
      res.end(JSON.stringify({ build: { version: "0.0.0-mock" } }));
      return;
    }
    if (req.method === "GET" && req.url === "/actuator/prometheus") {
      res.writeHead(200, { "content-type": "text/plain" });
      res.end("# HELP mock_metric Mock metric\n# TYPE mock_metric gauge\nmock_metric 1\n");
      return;
    }
    if (req.method === "POST" && req.url === "/graphql") {
      const chunks = [];
      req.on("data", (chunk) => chunks.push(chunk));
      req.on("end", () => handleGraphql(req, res, Buffer.concat(chunks).toString("utf8")));
      return;
    }
    res.writeHead(404);
    res.end();
  });
  return new Promise((resolve) => {
    server.listen(port, "127.0.0.1", () => {
      resolve({ server, port: server.address().port });
    });
  });
}

if (process.argv[1] && process.argv[1].endsWith("mock-server.js")) {
  const port = Number(process.argv[2]) || 8099;
  startMockServer(port).then(() => {
    process.stdout.write(`mock gateway listening on http://127.0.0.1:${port}\n`);
  });
}

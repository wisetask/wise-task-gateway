const COLORS = ["GRAY", "RED", "BLUE", "GREEN"];

const PAGINATION_FIELDS = "page pageSize totalCount totalPages hasNext hasPrevious";

const PROFILE_FIELDS = "id email firstName lastName patronymic profileRole";

function randomInt(max) {
  return Math.floor(Math.random() * max);
}

export function buildGraphPayload(vertexCount, edgeCount, isDirect) {
  const vertexList = [];
  for (let i = 0; i < vertexCount; i++) {
    vertexList.push({
      id: i,
      weight: randomInt(10),
      label: `v${i}`,
      xCoordinate: randomInt(800),
      yCoordinate: randomInt(600),
      color: COLORS[randomInt(COLORS.length)]
    });
  }
  const edgeList = [];
  for (let i = 0; i < edgeCount; i++) {
    const source = randomInt(vertexCount);
    const target = isDirect ? randomInt(vertexCount) : (source + 1 + randomInt(vertexCount - 1)) % vertexCount;
    edgeList.push({
      source,
      target,
      weight: randomInt(10),
      label: `e${i}`,
      color: COLORS[randomInt(COLORS.length)]
    });
  }
  return { vertexCount, edgeCount, isDirect, vertexList, edgeList, isNamed: false, name: null };
}

export const OPS = [
  {
    name: "signIn",
    group: "auth",
    needsAuth: false,
    weight: 2,
    variables: (ctx) => ({
      req: {
        email: ctx.config.email || "nonexistent@loadtest.local",
        password: ctx.config.password || "invalid-password"
      }
    }),
    expectErrors: (ctx) => ctx.config.email === null,
    query: `mutation SignIn($req: SignInRequest!) {
  signIn(signInRequest: $req) { accessToken refreshToken }
}`
  },
  {
    name: "refreshToken",
    group: "auth",
    needsAuth: false,
    weight: 2,
    variables: (ctx) => ({ refreshToken: ctx.refreshToken || "invalid" }),
    expectErrors: (ctx) => ctx.refreshToken === null,
    query: `mutation RefreshToken($refreshToken: String!) {
  refreshToken(refreshToken: $refreshToken) { accessToken refreshToken }
}`
  },
  {
    name: "signUp",
    group: "auth",
    needsAuth: false,
    weight: 1,
    enabled: (ctx) => ctx.config.signUpEnabled,
    variables: () => ({
      req: {
        profile: {
          email: `loadtest-${Date.now()}-${randomInt(1e9)}@loadtest.local`,
          profilePassword: "Loadtest-P4ss",
          firstName: "Load",
          lastName: "Test",
          profileRole: "USER"
        }
      }
    }),
    query: `mutation SignUp($req: SignUpRequest!) {
  signUp(signUpRequest: $req) { accessToken refreshToken }
}`
  },
  {
    name: "getMyProfile",
    group: "read",
    needsAuth: true,
    weight: 5,
    query: `query GetMyProfile {
  getMyProfile { ${PROFILE_FIELDS} }
}`
  },
  {
    name: "getAllProfiles",
    group: "read",
    needsAuth: true,
    weight: 5,
    variables: () => ({
      req: { pagination: { page: 0, pageSize: 20 }, filter: {} }
    }),
    query: `query GetAllProfiles($req: GetAllProfilesRequestInput!) {
  getAllProfiles(request: $req) {
    items { ${PROFILE_FIELDS} }
    pagination { ${PAGINATION_FIELDS} }
  }
}`
  },
  {
    name: "getAllTasks",
    group: "read",
    needsAuth: true,
    weight: 20,
    variables: () => ({
      req: { pagination: { page: 0, pageSize: 20 }, filter: {} }
    }),
    query: `query GetAllTasks($req: GetAllTaskRequestInput!) {
  getAllTasks(request: $req) {
    items { id name category taskType isPublic }
    pagination { ${PAGINATION_FIELDS} }
  }
}`
  },
  {
    name: "getTask",
    group: "read",
    needsAuth: true,
    weight: 10,
    enabled: (ctx) => Boolean(ctx.sample.taskId),
    variables: (ctx) => ({ id: ctx.sample.taskId }),
    query: `query GetTask($id: ID!) {
  getTask(id: $id) {
    ... on TaskGraph { id name description category taskType isPublic isHiddenMistake rule { isColor isEdit isMove isDelete } }
    ... on TaskImplementation { id name description category taskType isPublic }
  }
}`
  },
  {
    name: "getAllTaskSolutions",
    group: "read",
    needsAuth: true,
    weight: 5,
    variables: () => ({
      req: { pagination: { page: 0, pageSize: 20 } }
    }),
    query: `query GetAllTaskSolutions($req: GetAllTaskSolutionsRequestInput!) {
  getAllTaskSolutions(request: $req) {
    items { id isCorrect }
    pagination { ${PAGINATION_FIELDS} }
  }
}`
  },
  {
    name: "getAllGraphs",
    group: "read",
    needsAuth: true,
    weight: 10,
    variables: () => ({
      req: { pagination: { page: 0, pageSize: 20 }, filter: {} }
    }),
    query: `query GetAllGraphs($req: GetAllGraphsRequestInput!) {
  getAllGraphs(request: $req) {
    items { id vertexCount edgeCount isDirect }
    pagination { ${PAGINATION_FIELDS} }
  }
}`
  },
  {
    name: "getGraphById",
    group: "read",
    needsAuth: true,
    weight: 10,
    enabled: (ctx) => Boolean(ctx.sample.graphId),
    variables: (ctx) => ({ id: ctx.sample.graphId }),
    query: `query GetGraphById($id: ID!) {
  getGraphById(id: $id) { id vertexCount edgeCount isDirect isNamed name }
}`
  },
  {
    name: "getAllPlugins",
    group: "read",
    needsAuth: true,
    weight: 10,
    variables: () => ({
      req: { pagination: { page: 0, pageSize: 20 }, filter: {} }
    }),
    query: `query GetAllPlugins($req: GetAllPluginRequestInput!) {
  getAllPlugins(request: $req) {
    items { id name category graphType isValid pluginType isInternal }
    pagination { ${PAGINATION_FIELDS} }
  }
}`
  },
  {
    name: "getPlugin",
    group: "read",
    needsAuth: true,
    weight: 5,
    enabled: (ctx) => Boolean(ctx.sample.pluginId),
    variables: (ctx) => ({ id: ctx.sample.pluginId }),
    query: `query GetPlugin($id: ID!) {
  getPlugin(id: $id) { id name description category graphType isValid pluginType isInternal }
}`
  },
  {
    name: "getStatistic-sum-user",
    group: "statistics",
    needsAuth: true,
    weight: 5,
    variables: (ctx) => ({
      req: { type: "SUM", scope: "USER", user_id: ctx.userId, event_type: "TASK_SOLVED" }
    }),
    query: `query GetStatisticSumUser($req: StatisticRequestInput!) {
  getStatistic(request: $req) { scope type value updated_at }
}`
  },
  {
    name: "getStatistic-success-task",
    group: "statistics",
    needsAuth: true,
    weight: 5,
    enabled: (ctx) => Boolean(ctx.sample.taskId),
    variables: (ctx) => ({
      req: { type: "SUCCESS_RATE", scope: "TASK", task_id: ctx.sample.taskId, event_type: "TASK_SOLVED" }
    }),
    query: `query GetStatisticSuccessTask($req: StatisticRequestInput!) {
  getStatistic(request: $req) { scope type value updated_at }
}`
  },
  {
    name: "generateGraph",
    group: "write",
    needsAuth: true,
    weight: 10,
    variables: (ctx) => ({
      req: {
        vertexCount: ctx.config.graphVertexCount,
        edgeCount: ctx.config.graphEdgeCount,
        isDirect: Math.random() < 0.5,
        isSaved: false
      }
    }),
    query: `mutation GenerateGraph($req: GenerateGraphRequest!) {
  generateGraph(generateGraphRequest: $req) { id vertexCount edgeCount isDirect }
}`
  },
  {
    name: "createDeleteGraph",
    group: "write-persistent",
    needsAuth: true,
    weight: 3,
    isComposite: true,
    enabled: (ctx) => ctx.config.writeEnabled,
    query: "",
    createVariables: (ctx) => ({
      graph: buildGraphPayload(ctx.config.graphVertexCount, ctx.config.graphEdgeCount, Math.random() < 0.5)
    }),
    createQuery: `mutation CreateGraph($graph: GraphInput!) {
  createGraph(graph: $graph) { id vertexCount }
}`,
    deleteQuery: `mutation DeleteGraph($id: ID!) {
  deleteGraph(id: $id)
}`
  },
  {
    name: "solveTaskGraph",
    group: "write-heavy",
    needsAuth: true,
    weight: 2,
    enabled: (ctx) => Boolean(ctx.config.solveTaskId),
    variables: (ctx) => ({
      solution: {
        taskId: ctx.config.solveTaskId,
        graph: buildGraphPayload(8, 8, false)
      }
    }),
    query: `mutation SolveTaskGraph($solution: SolutionGraphInput!) {
  solveTaskGraph(solution: $solution) { id isCorrect }
}`
  },
  {
    name: "getMyProfile-unauthorized",
    group: "security",
    needsAuth: false,
    noAuthHeader: true,
    weightKey: "unauthorizedWeight",
    expectErrors: () => true,
    query: `query GetMyProfileAnon {
  getMyProfile { id email }
}`
  },
  {
    name: "actuator-health",
    group: "infra",
    isRest: true,
    weightKey: "healthWeight",
    method: "GET",
    path: "/actuator/health",
    validate: (body) => (body.includes("UP") ? null : "health status is not UP")
  },
  {
    name: "actuator-prometheus",
    group: "infra",
    isRest: true,
    weightKey: "prometheusWeight",
    method: "GET",
    path: "/actuator/prometheus",
    validate: (body) => (body.includes("# HELP") || body.length > 0 ? null : "empty prometheus payload")
  }
];

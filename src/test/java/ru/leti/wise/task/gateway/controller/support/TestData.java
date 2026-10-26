package ru.leti.wise.task.gateway.controller.support;

import com.google.protobuf.Timestamp;
import ru.leti.graphql.types.GraphInput;
import ru.leti.graphql.types.PluginInfoInput;
import ru.leti.graphql.types.RuleInput;
import ru.leti.graphql.types.TaskGraphInput;
import ru.leti.graphql.types.VertexInput;
import ru.leti.wise.task.event.Statistic;
import ru.leti.wise.task.graph.GraphOuterClass;
import ru.leti.wise.task.pagination.Pagination;
import ru.leti.wise.task.plugin.PluginOuterClass;
import ru.leti.wise.task.profile.ProfileOuterClass;
import ru.leti.wise.task.task.TaskOuterClass;

import java.util.List;

/**
 * Фабрика тестовых данных: собирает protobuf-сообщения (то, что отдают gRPC-сервисы)
 * и GraphQL-инпуты (то, что приходит в контроллеры).
 */
public final class TestData {

    private TestData() {
    }

    public static ProfileOuterClass.Profile profile(String id, String email) {
        return ProfileOuterClass.Profile.newBuilder()
                .setId(id)
                .setEmail(email)
                .setFirstName("First " + id)
                .setLastName("Last " + id)
                .setPatronymic("Patronymic " + id)
                .setProfileRole(ProfileOuterClass.Role.AUTHOR)
                .build();
    }

    public static GraphOuterClass.Graph graph(String id, String authorId) {
        return GraphOuterClass.Graph.newBuilder()
                .setId(id)
                .setAuthorId(authorId)
                .setName("graph " + id)
                .setVertexCount(1)
                .setEdgeCount(1)
                .setIsDirect(true)
                .setIsNamed(true)
                .addVertexList(GraphOuterClass.Vertex.newBuilder()
                        .setId(1)
                        .setWeight(5)
                        .setLabel("vertex 1")
                        .setXCoordinate(10)
                        .setYCoordinate(20)
                        .setColor(GraphOuterClass.Color.RED)
                        .build())
                .addEdgeList(GraphOuterClass.Edge.newBuilder()
                        .setSource(1)
                        .setTarget(2)
                        .setWeight(7)
                        .setLabel("edge 1")
                        .setColor(GraphOuterClass.Color.BLUE)
                        .build())
                .build();
    }

    public static PluginOuterClass.Plugin plugin(String id, String authorId) {
        return PluginOuterClass.Plugin.newBuilder()
                .setId(id)
                .setName("plugin " + id)
                .setDescription("plugin description " + id)
                .setCategory("category")
                .setAuthorId(authorId)
                .setGraphType(PluginOuterClass.GraphType.DIRECT)
                .setIsValid(true)
                .setBeanName("bean " + id)
                .setPluginType(PluginOuterClass.PluginType.GRAPH_PROPERTY)
                .setIsInternal(true)
                .build();
    }

    public static TaskOuterClass.PluginInfo pluginInfo(String pluginId) {
        return TaskOuterClass.PluginInfo.newBuilder()
                .setPluginId(pluginId)
                .setValue("true")
                .setMistakeText("mistake")
                .setSign("sign")
                .setPluginType(TaskOuterClass.PluginType.GRAPH_PROPERTY)
                .setOrder(1)
                .build();
    }

    public static TaskOuterClass.Task taskGraph(String id, String authorId, List<String> pluginIds) {
        return TaskOuterClass.Task.newBuilder()
                .setId(id)
                .setName(id + " name")
                .setDescription(id + " description")
                .setCategory("category")
                .setTaskType(TaskOuterClass.TaskType.GRAPH)
                .setAuthorId(authorId)
                .setIsPublic(true)
                .setTaskGraph(TaskOuterClass.TaskGraph.newBuilder()
                        .setIsHiddenMistake(true)
                        .setGraph(graph("graph-" + id, authorId))
                        .setRule(TaskOuterClass.Rule.newBuilder()
                                .setIsColor(true)
                                .setIsEdit(true)
                                .setIsMove(true)
                                .setIsDelete(false)
                                .build())
                        .addAllCondition(pluginIds.stream().map(TestData::pluginInfo).toList())
                        .build())
                .build();
    }

    public static TaskOuterClass.Task taskImplementation(String id, String authorId, String pluginId) {
        return TaskOuterClass.Task.newBuilder()
                .setId(id)
                .setName(id + " name")
                .setDescription(id + " description")
                .setCategory("category")
                .setTaskType(TaskOuterClass.TaskType.IMPLEMENTATION)
                .setAuthorId(authorId)
                .setIsPublic(false)
                .setTaskImplementation(TaskOuterClass.TaskImplementation.newBuilder()
                        .setPluginId(pluginId)
                        .build())
                .build();
    }

    public static TaskOuterClass.PluginResult pluginResult(String pluginId) {
        return TaskOuterClass.PluginResult.newBuilder()
                .setPluginId(pluginId)
                .setIsCorrect(true)
                .setValue("value")
                .setTrueValue("true value")
                .setPluginMessage("message")
                .build();
    }

    public static TaskOuterClass.Solution solutionGraph(String id, String taskId, String authorId,
                                                        List<String> pluginIds) {
        return TaskOuterClass.Solution.newBuilder()
                .setId(id)
                .setTaskId(taskId)
                .setAuthorId(authorId)
                .setIsCorrect(true)
                .setSolutionGraph(TaskOuterClass.SolutionGraph.newBuilder()
                        .setGraph(graph("solution-graph-" + id, authorId))
                        .addAllPluginResults(pluginIds.stream().map(TestData::pluginResult).toList())
                        .build())
                .build();
    }

    public static TaskOuterClass.Solution solutionImplementation(String id, String taskId, String authorId) {
        return TaskOuterClass.Solution.newBuilder()
                .setId(id)
                .setTaskId(taskId)
                .setAuthorId(authorId)
                .setIsCorrect(false)
                .setSolutionImplementation(TaskOuterClass.SolutionImplementation.newBuilder()
                        .setCode("print('hello')")
                        .addImplementationResult(TaskOuterClass.GraphResult.newBuilder()
                                .setId("result-1")
                                .setOriginalTimeResult(1.5)
                                .setTimeResult(1.0)
                                .setOriginalResult("original")
                                .setResult("result")
                                .build())
                        .build())
                .build();
    }

    public static Pagination.PaginationResponse pagination() {
        return Pagination.PaginationResponse.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .setTotalCount(1)
                .setTotalPages(1)
                .setHasNext(false)
                .setHasPrevious(false)
                .build();
    }

    public static PluginOuterClass.GraphTestResult graphTestResult(String graphId) {
        return PluginOuterClass.GraphTestResult.newBuilder()
                .setGraphId(graphId)
                .setOriginalTimeResult(100)
                .setTimeResult(50)
                .setResult("result")
                .setOriginalResult("original result")
                .build();
    }

    public static PluginOuterClass.ImplementationResult implementationResult(PluginOuterClass.GraphTestResult... results) {
        return PluginOuterClass.ImplementationResult.newBuilder()
                .setResult(true)
                .addAllGraphTestResults(List.of(results))
                .build();
    }

    public static Statistic.StatisticResponse statisticResponse() {
        return Statistic.StatisticResponse.newBuilder()
                .setScope(Statistic.StatisticScope.TASK)
                .setType(Statistic.StatisticType.SUCCESS_RATE)
                .setEventType("SOLVE")
                .setTaskId("task-1")
                .setUserId("user-1")
                .setValue(0.75)
                .setUpdateAt(Timestamp.newBuilder().setSeconds(1_700_000_000L).build())
                .build();
    }

    public static GraphInput graphInput(String id) {
        var vertex = new VertexInput();
        vertex.setId(1);
        vertex.setWeight(2);
        vertex.setLabel("v");
        vertex.setXCoordinate(1);
        vertex.setYCoordinate(2);
        vertex.setColor(ru.leti.graphql.types.Color.RED);

        var graphInput = new GraphInput();
        graphInput.setId(id);
        graphInput.setName("graph input " + id);
        graphInput.setVertexCount(1);
        graphInput.setEdgeCount(0);
        graphInput.setIsDirect(true);
        graphInput.setIsNamed(true);
        graphInput.setVertexList(List.of(vertex));
        graphInput.setEdgeList(List.of());

        return graphInput;
    }

    public static TaskGraphInput taskGraphInput(String id, String authorId, String pluginId) {
        var rule = new RuleInput();
        rule.setIsColor(true);
        rule.setIsEdit(true);
        rule.setIsMove(true);
        rule.setIsDelete(false);

        var pluginInfo = new PluginInfoInput();
        pluginInfo.setPluginId(pluginId);
        pluginInfo.setValue("true");
        pluginInfo.setMistakeText("mistake");
        pluginInfo.setSign("sign");
        pluginInfo.setPluginType(ru.leti.graphql.types.PluginType.GRAPH_PROPERTY);
        pluginInfo.setOrder(1);

        var input = new TaskGraphInput();
        input.setId(id);
        input.setName(id + " name");
        input.setDescription(id + " description");
        input.setCategory("category");
        input.setTaskType(ru.leti.graphql.types.TaskType.GRAPH);
        input.setAuthorId(authorId);
        input.setIsPublic(true);
        input.setIsHiddenMistake(false);
        input.setGraph(graphInput("graph-" + id));
        input.setRule(rule);
        input.setCondition(List.of(pluginInfo));

        return input;
    }
}

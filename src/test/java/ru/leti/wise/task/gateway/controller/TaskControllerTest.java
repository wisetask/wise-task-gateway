package ru.leti.wise.task.gateway.controller;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import ru.leti.graphql.types.Color;
import ru.leti.graphql.types.GetAllTaskRequestInput;
import ru.leti.graphql.types.GetAllTaskSolutionsRequestInput;
import ru.leti.graphql.types.SolutionGraph;
import ru.leti.graphql.types.Task;
import ru.leti.graphql.types.TaskGraph;
import ru.leti.graphql.types.TaskImplementation;
import ru.leti.graphql.types.TaskType;
import ru.leti.wise.task.gateway.controller.support.AbstractControllerTest;
import ru.leti.wise.task.gateway.controller.support.TestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskControllerTest extends AbstractControllerTest {

    private TaskController controller() {
        return controller(TaskController.class);
    }

    @Test
    void getTask_mapsTaskGraphWithAuthorGraphRuleAndConditionPlugins() {
        taskGrpcService.putTask(TestData.taskGraph("task-1", "author-1", List.of("plugin-1")));
        profileGrpcService.putProfile(TestData.profile("author-1", "author@example.com"));
        pluginGrpcService.putPlugin(TestData.plugin("plugin-1", "author-1"));

        Task task = controller().getTask("task-1");

        assertThat(task).isInstanceOf(TaskGraph.class);
        var taskGraph = (TaskGraph) task;
        assertThat(taskGraph.getId()).isEqualTo("task-1");
        assertThat(taskGraph.getName()).isEqualTo("task-1 name");
        assertThat(taskGraph.getTaskType()).isEqualTo(TaskType.GRAPH);
        assertThat(taskGraph.getIsPublic()).isTrue();
        assertThat(taskGraph.getIsHiddenMistake()).isTrue();

        assertThat(taskGraph.getAuthor()).isNotNull();
        assertThat(taskGraph.getAuthor().getId()).isEqualTo("author-1");
        assertThat(taskGraph.getAuthor().getEmail()).isEqualTo("author@example.com");

        assertThat(taskGraph.getGraph()).isNotNull();
        assertThat(taskGraph.getGraph().getId()).isEqualTo("graph-task-1");
        assertThat(taskGraph.getGraph().getVertexCount()).isEqualTo(1);
        assertThat(taskGraph.getGraph().getEdgeCount()).isEqualTo(1);
        assertThat(taskGraph.getGraph().getVertexList()).hasSize(1);
        assertThat(taskGraph.getGraph().getVertexList().get(0).getId()).isEqualTo(1);
        assertThat(taskGraph.getGraph().getVertexList().get(0).getLabel()).isEqualTo("vertex 1");
        assertThat(taskGraph.getGraph().getVertexList().get(0).getXCoordinate()).isEqualTo(10);
        assertThat(taskGraph.getGraph().getVertexList().get(0).getYCoordinate()).isEqualTo(20);
        assertThat(taskGraph.getGraph().getVertexList().get(0).getColor()).isEqualTo(Color.RED);
        assertThat(taskGraph.getGraph().getEdgeList()).hasSize(1);
        assertThat(taskGraph.getGraph().getEdgeList().get(0).getSource()).isEqualTo(1);
        assertThat(taskGraph.getGraph().getEdgeList().get(0).getLabel()).isEqualTo("edge 1");

        assertThat(taskGraph.getRule().getIsMove()).isTrue();
        assertThat(taskGraph.getRule().getIsDelete()).isFalse();

        assertThat(taskGraph.getCondition()).hasSize(1);
        assertThat(taskGraph.getCondition().get(0).getValue()).isEqualTo("true");
        assertThat(taskGraph.getCondition().get(0).getPlugin()).isNotNull();
        assertThat(taskGraph.getCondition().get(0).getPlugin().getId()).isEqualTo("plugin-1");
        assertThat(taskGraph.getCondition().get(0).getPlugin().getAuthor().getId()).isEqualTo("author-1");
    }

    @Test
    void getTask_mapsTaskImplementationWithAuthorAndPlugin() {
        taskGrpcService.putTask(TestData.taskImplementation("task-2", "author-2", "plugin-2"));
        profileGrpcService.putProfile(TestData.profile("author-2", "author2@example.com"));
        pluginGrpcService.putPlugin(TestData.plugin("plugin-2", "author-2"));

        Task task = controller().getTask("task-2");

        assertThat(task).isInstanceOf(TaskImplementation.class);
        var implementation = (TaskImplementation) task;
        assertThat(implementation.getId()).isEqualTo("task-2");
        assertThat(implementation.getTaskType()).isEqualTo(TaskType.IMPLEMENTATION);
        assertThat(implementation.getAuthor().getId()).isEqualTo("author-2");
        assertThat(implementation.getPlugin()).isNotNull();
        assertThat(implementation.getPlugin().getId()).isEqualTo("plugin-2");
        assertThat(implementation.getPlugin().getAuthor().getId()).isEqualTo("author-2");
    }

    @Test
    void getTask_whenTaskMissing_propagatesGrpcError() {
        assertThatThrownBy(() -> controller().getTask("missing"))
                .isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    void getTask_whenProfileServiceFails_returnsNullAuthorWithoutNpe() {
        taskGrpcService.putTask(TestData.taskGraph("task-1", "author-1", List.of("plugin-1")));
        profileGrpcService.failGetProfile();
        pluginGrpcService.putPlugin(TestData.plugin("plugin-1", "author-1"));

        var taskGraph = (TaskGraph) controller().getTask("task-1");

        assertThat(taskGraph.getAuthor()).isNull();
        assertThat(taskGraph.getGraph().getId()).isEqualTo("graph-task-1");
        assertThat(taskGraph.getCondition()).hasSize(1);
    }

    @Test
    void getTask_whenPluginServiceFails_returnsNullPluginWithoutNpe() {
        taskGrpcService.putTask(TestData.taskImplementation("task-2", "author-2", "plugin-2"));
        profileGrpcService.putProfile(TestData.profile("author-2", "author2@example.com"));
        pluginGrpcService.failGetPlugin();

        var implementation = (TaskImplementation) controller().getTask("task-2");

        assertThat(implementation.getAuthor().getId()).isEqualTo("author-2");
        assertThat(implementation.getPlugin()).isNull();
    }

    @Test
    void getAllTasks_mapsItemsAndToleratesMissingAuthors() {
        taskGrpcService.putTask(TestData.taskGraph("task-1", "author-1", List.of("plugin-1")));
        taskGrpcService.putTask(TestData.taskImplementation("task-2", "author-2", "plugin-2"));
        profileGrpcService.putProfile(TestData.profile("author-1", "author1@example.com"));
        pluginGrpcService.putPlugin(TestData.plugin("plugin-1", "author-1"));
        pluginGrpcService.putPlugin(TestData.plugin("plugin-2", "author-1"));

        var response = controller().getAllTasks(GetAllTaskRequestInput.newBuilder().build());

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getPagination().getTotalCount()).isEqualTo(1L);
        assertThat(response.getItems().get(0).getAuthor().getId()).isEqualTo("author-1");
        assertThat(response.getItems().get(1).getAuthor()).isNull();
        assertThat(response.getItems().get(1)).isInstanceOf(TaskImplementation.class);
    }

    @Test
    void getTaskSolution_mapsSolutionGraphWithTaskAuthorGraphAndPluginResults() {
        taskGrpcService.putTask(TestData.taskGraph("task-1", "author-1", List.of()));
        taskGrpcService.putSolution(TestData.solutionGraph("solution-1", "task-1", "author-1", List.of("plugin-1")));
        profileGrpcService.putProfile(TestData.profile("author-1", "author@example.com"));
        pluginGrpcService.putPlugin(TestData.plugin("plugin-1", "author-1"));

        var solution = controller().getTaskSolution("solution-1");

        assertThat(solution).isInstanceOf(SolutionGraph.class);
        var solutionGraph = (SolutionGraph) solution;
        assertThat(solutionGraph.getId()).isEqualTo("solution-1");
        assertThat(solutionGraph.getIsCorrect()).isTrue();
        assertThat(solutionGraph.getGraph().getId()).isEqualTo("solution-graph-solution-1");
        assertThat(solutionGraph.getAuthor().getId()).isEqualTo("author-1");
        assertThat(solutionGraph.getTask()).isNotNull();
        assertThat(solutionGraph.getTask().getId()).isEqualTo("task-1");
        assertThat(solutionGraph.getPluginResults()).hasSize(1);
        assertThat(solutionGraph.getPluginResults().get(0).getValue()).isEqualTo("value");
        assertThat(solutionGraph.getPluginResults().get(0).getPlugin()).isNotNull();
        assertThat(solutionGraph.getPluginResults().get(0).getPlugin().getId()).isEqualTo("plugin-1");
    }

    @Test
    void getTaskSolution_whenPluginServiceFails_returnsNullPluginWithoutNpe() {
        taskGrpcService.putTask(TestData.taskGraph("task-1", "author-1", List.of()));
        taskGrpcService.putSolution(TestData.solutionGraph("solution-1", "task-1", "author-1", List.of("plugin-1")));
        profileGrpcService.putProfile(TestData.profile("author-1", "author@example.com"));
        pluginGrpcService.failGetPluginsByIds();

        var solutionGraph = (SolutionGraph) controller().getTaskSolution("solution-1");

        assertThat(solutionGraph.getAuthor().getId()).isEqualTo("author-1");
        assertThat(solutionGraph.getPluginResults()).hasSize(1);
        assertThat(solutionGraph.getPluginResults().get(0).getPlugin()).isNull();
    }

    @Test
    void getTaskSolution_whenTaskServiceFails_returnsNullTaskWithoutNpe() {
        taskGrpcService.putSolution(TestData.solutionGraph("solution-1", "missing-task", "author-1", List.of()));
        profileGrpcService.putProfile(TestData.profile("author-1", "author@example.com"));

        var solutionGraph = (SolutionGraph) controller().getTaskSolution("solution-1");

        assertThat(solutionGraph.getTask()).isNull();
        assertThat(solutionGraph.getAuthor().getId()).isEqualTo("author-1");
    }

    @Test
    void createTaskGraph_mapsCreatedTaskWithCurrentUserAndConditionPlugins() {
        profileGrpcService.putProfile(TestData.profile(CURRENT_USER_ID, "current@example.com"));
        pluginGrpcService.putPlugin(TestData.plugin("plugin-1", CURRENT_USER_ID));

        var created = controller().createTaskGraph(TestData.taskGraphInput("task-new", CURRENT_USER_ID, "plugin-1"));

        assertThat(created.getId()).isEqualTo("task-new");
        assertThat(created.getName()).isEqualTo("task-new name");
        assertThat(created.getTaskType()).isEqualTo(TaskType.GRAPH);
        assertThat(created.getAuthor().getId()).isEqualTo(CURRENT_USER_ID);
        assertThat(created.getRule().getIsMove()).isTrue();
        assertThat(created.getIsPublic()).isTrue();
        assertThat(created.getCondition()).hasSize(1);
        assertThat(created.getCondition().get(0).getValue()).isEqualTo("true");
        assertThat(created.getCondition().get(0).getPlugin()).isNotNull();
        assertThat(created.getCondition().get(0).getPlugin().getId()).isEqualTo("plugin-1");
        assertThat(created.getGraph()).isNotNull();
        assertThat(created.getGraph().getVertexList()).hasSize(1);
        assertThat(created.getGraph().getEdgeList()).isEmpty();
    }

    @Test
    void getAllTaskSolutions_mapsTasksAuthorsAndPagination() {
        taskGrpcService.putTask(TestData.taskGraph("task-1", "author-1", List.of()));
        taskGrpcService.putSolution(TestData.solutionGraph("solution-1", "task-1", "author-1", List.of("plugin-1")));
        profileGrpcService.putProfile(TestData.profile("author-1", "author@example.com"));
        pluginGrpcService.putPlugin(TestData.plugin("plugin-1", "author-1"));

        var response = controller().getAllTaskSolutions(GetAllTaskSolutionsRequestInput.newBuilder().build());

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getPagination().getTotalCount()).isEqualTo(1L);
        assertThat(response.getItems().get(0).getTask().getId()).isEqualTo("task-1");
        assertThat(response.getItems().get(0).getAuthor().getId()).isEqualTo("author-1");
    }

    @Test
    void deleteTask_returnsDeletedId() {
        taskGrpcService.putTask(TestData.taskGraph("task-1", "author-1", List.of()));
        assertThat(controller().deleteTask("task-1")).isEqualTo("task-1");
    }
}

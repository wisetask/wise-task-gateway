package ru.leti.wise.task.gateway.controller;

import org.junit.jupiter.api.Test;
import ru.leti.graphql.types.GetAllPluginRequestInput;
import ru.leti.graphql.types.GraphType;
import ru.leti.graphql.types.Payload;
import ru.leti.graphql.types.PayloadType;
import ru.leti.graphql.types.Plugin;
import ru.leti.graphql.types.PluginInput;
import ru.leti.graphql.types.PluginType;
import ru.leti.graphql.types.SolutionInput;
import ru.leti.wise.task.gateway.controller.support.AbstractControllerTest;
import ru.leti.wise.task.gateway.controller.support.TestData;

import static org.assertj.core.api.Assertions.assertThat;

class PluginControllerTest extends AbstractControllerTest {

    private PluginController controller() {
        return controller(PluginController.class);
    }

    @Test
    void getPlugin_mapsPluginWithAuthor() {
        pluginGrpcService.putPlugin(TestData.plugin("plugin-1", "author-1"));
        profileGrpcService.putProfile(TestData.profile("author-1", "author@example.com"));

        Plugin plugin = controller().getPlugin("plugin-1");

        assertThat(plugin.getId()).isEqualTo("plugin-1");
        assertThat(plugin.getName()).isEqualTo("plugin plugin-1");
        assertThat(plugin.getDescription()).isEqualTo("plugin description plugin-1");
        assertThat(plugin.getCategory()).isEqualTo("category");
        assertThat(plugin.getGraphType()).isEqualTo(GraphType.DIRECT);
        assertThat(plugin.getPluginType()).isEqualTo(PluginType.GRAPH_PROPERTY);
        assertThat(plugin.getIsValid()).isTrue();
        assertThat(plugin.getIsInternal()).isTrue();
        assertThat(plugin.getBeanName()).isEqualTo("bean plugin-1");
        assertThat(plugin.getAuthor()).isNotNull();
        assertThat(plugin.getAuthor().getId()).isEqualTo("author-1");
    }

    @Test
    void getPlugin_whenProfileServiceFails_returnsNullAuthorWithoutNpe() {
        pluginGrpcService.putPlugin(TestData.plugin("plugin-1", "author-1"));
        profileGrpcService.failGetProfile();

        Plugin plugin = controller().getPlugin("plugin-1");

        assertThat(plugin.getId()).isEqualTo("plugin-1");
        assertThat(plugin.getAuthor()).isNull();
    }

    @Test
    void getAllPlugins_mapsItemsAndToleratesMissingAuthors() {
        pluginGrpcService.putPlugin(TestData.plugin("plugin-1", "author-1"));
        pluginGrpcService.putPlugin(TestData.plugin("plugin-2", "author-2"));
        profileGrpcService.putProfile(TestData.profile("author-1", "author1@example.com"));

        var response = controller().getAllPlugins(GetAllPluginRequestInput.newBuilder().build());

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getPagination().getTotalCount()).isEqualTo(1L);
        assertThat(response.getItems().get(0).getAuthor().getId()).isEqualTo("author-1");
        assertThat(response.getItems().get(1).getAuthor()).isNull();
    }

    @Test
    void createPlugin_mapsCreatedPluginWithCurrentUser() {
        profileGrpcService.putProfile(TestData.profile(CURRENT_USER_ID, "current@example.com"));

        var input = new PluginInput();
        input.setId("plugin-new");
        input.setName("new plugin");
        input.setDescription("new plugin description");
        input.setCategory("category");
        input.setAuthorId(CURRENT_USER_ID);
        input.setIsValid(true);
        input.setGraphType(GraphType.UNDIRECT);
        input.setPluginType(PluginType.GRAPH_STRING);
        input.setIsInternal(false);

        Plugin created = controller().createPlugin(input);

        assertThat(created.getId()).isEqualTo("plugin-new");
        assertThat(created.getName()).isEqualTo("new plugin");
        assertThat(created.getGraphType()).isEqualTo(GraphType.UNDIRECT);
        assertThat(created.getPluginType()).isEqualTo(PluginType.GRAPH_STRING);
        assertThat(created.getIsInternal()).isFalse();
        assertThat(created.getAuthor().getId()).isEqualTo(CURRENT_USER_ID);
    }

    @Test
    void checkPluginImplementation_mapsGraphTestResults() {
        graphGrpcService.putGraph(TestData.graph("graph-1", "author-1"));
        profileGrpcService.putProfile(TestData.profile("author-1", "author@example.com"));

        var result = controller().checkPluginImplementation("plugin-1", "file.jar");

        assertThat(result.getResult()).isTrue();
        assertThat(result.getGraphTestResults()).hasSize(1);
        var graphTestResult = result.getGraphTestResults().get(0);
        assertThat(graphTestResult.getResult()).isEqualTo("result");
        assertThat(graphTestResult.getOriginalResult()).isEqualTo("original result");
        assertThat(graphTestResult.getOriginalTimeResult()).isEqualTo(100);
        assertThat(graphTestResult.getTimeResult()).isEqualTo(50);
        assertThat(graphTestResult.getGraph()).isNotNull();
        assertThat(graphTestResult.getGraph().getId()).isEqualTo("graph-1");
    }

    @Test
    void checkPluginImplementation_whenGraphServiceFails_returnsNullGraphWithoutNpe() {
        graphGrpcService.failGetGraphsByIds();

        var result = controller().checkPluginImplementation("plugin-1", "file.jar");

        assertThat(result.getResult()).isTrue();
        assertThat(result.getGraphTestResults()).hasSize(1);
        assertThat(result.getGraphTestResults().get(0).getGraph()).isNull();
    }

    @Test
    void checkPluginSolution_mapsPayloadGraphWithVertices() {
        var payload = new Payload();
        payload.setDiscriminator(PayloadType.GRAPH);
        payload.setGraph(TestData.graphInput("payload-graph"));

        var input = new SolutionInput();
        input.setPluginId("plugin-1");
        input.setPluginType(PluginType.GRAPH_PROPERTY);
        input.setPayload(payload);

        assertThat(controller().checkPluginSolution(input)).isEqualTo("OK");

        var grpcSolution = pluginGrpcService.getLastCheckedSolution();
        assertThat(grpcSolution).isNotNull();
        assertThat(grpcSolution.getPluginId()).isEqualTo("plugin-1");
        assertThat(grpcSolution.hasGraph()).isTrue();
        assertThat(grpcSolution.getGraph().getVertexListCount()).isEqualTo(1);
        assertThat(grpcSolution.getGraph().getVertexList(0).getLabel()).isEqualTo("v");
        assertThat(grpcSolution.getGraph().getVertexList(0).getXCoordinate()).isEqualTo(1);
        assertThat(grpcSolution.getGraph().getVertexList(0).getYCoordinate()).isEqualTo(2);
    }

    @Test
    void validatePlugin_returnsValidatedId() {
        assertThat(controller().validatePlugin("plugin-1")).isEqualTo("plugin-1");
    }

    @Test
    void deletePlugin_returnsDeletedId() {
        pluginGrpcService.putPlugin(TestData.plugin("plugin-1", "author-1"));

        assertThat(controller().deletePlugin("plugin-1")).isEqualTo("plugin-1");
    }
}

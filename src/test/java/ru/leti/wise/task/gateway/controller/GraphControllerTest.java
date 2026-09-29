package ru.leti.wise.task.gateway.controller;

import org.junit.jupiter.api.Test;
import ru.leti.graphql.types.Color;
import ru.leti.graphql.types.GenerateGraphRequest;
import ru.leti.graphql.types.GetAllGraphsRequestInput;
import ru.leti.graphql.types.Graph;
import ru.leti.wise.task.gateway.controller.support.AbstractControllerTest;
import ru.leti.wise.task.gateway.controller.support.TestData;

import static org.assertj.core.api.Assertions.assertThat;

class GraphControllerTest extends AbstractControllerTest {

    private GraphController controller() {
        return controller(GraphController.class);
    }

    @Test
    void getGraphById_mapsGraphWithAuthor() {
        graphGrpcService.putGraph(TestData.graph("graph-1", "author-1"));
        profileGrpcService.putProfile(TestData.profile("author-1", "author@example.com"));

        Graph graph = controller().getGraphById("graph-1");

        assertThat(graph.getId()).isEqualTo("graph-1");
        assertThat(graph.getName()).isEqualTo("graph graph-1");
        assertThat(graph.getVertexCount()).isEqualTo(1);
        assertThat(graph.getEdgeCount()).isEqualTo(1);
        assertThat(graph.getIsDirect()).isTrue();
        assertThat(graph.getIsNamed()).isTrue();
        assertThat(graph.getVertexList()).hasSize(1);
        assertThat(graph.getVertexList().get(0).getWeight()).isEqualTo(5);
        assertThat(graph.getVertexList().get(0).getLabel()).isEqualTo("vertex 1");
        assertThat(graph.getVertexList().get(0).getXCoordinate()).isEqualTo(10);
        assertThat(graph.getVertexList().get(0).getYCoordinate()).isEqualTo(20);
        assertThat(graph.getVertexList().get(0).getColor()).isEqualTo(Color.RED);
        assertThat(graph.getEdgeList()).hasSize(1);
        assertThat(graph.getEdgeList().get(0).getSource()).isEqualTo(1);
        assertThat(graph.getEdgeList().get(0).getTarget()).isEqualTo(2);
        assertThat(graph.getEdgeList().get(0).getWeight()).isEqualTo(7);
        assertThat(graph.getEdgeList().get(0).getLabel()).isEqualTo("edge 1");
        assertThat(graph.getEdgeList().get(0).getColor()).isEqualTo(Color.BLUE);
        assertThat(graph.getAuthor()).isNotNull();
        assertThat(graph.getAuthor().getId()).isEqualTo("author-1");
        assertThat(graph.getAuthor().getEmail()).isEqualTo("author@example.com");
    }

    @Test
    void getGraphById_whenProfileServiceFails_returnsNullAuthorWithoutNpe() {
        graphGrpcService.putGraph(TestData.graph("graph-1", "author-1"));
        profileGrpcService.failGetProfile();

        Graph graph = controller().getGraphById("graph-1");

        assertThat(graph.getId()).isEqualTo("graph-1");
        assertThat(graph.getAuthor()).isNull();
    }

    @Test
    void getAllGraphs_mapsItemsAndToleratesMissingAuthors() {
        graphGrpcService.putGraph(TestData.graph("graph-1", "author-1"));
        graphGrpcService.putGraph(TestData.graph("graph-2", "author-2"));
        profileGrpcService.putProfile(TestData.profile("author-1", "author1@example.com"));

        var response = controller().getAllGraphs(GetAllGraphsRequestInput.newBuilder().build());

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getPagination().getTotalCount()).isEqualTo(1L);
        assertThat(response.getItems().get(0).getId()).isEqualTo("graph-1");
        assertThat(response.getItems().get(0).getAuthor().getId()).isEqualTo("author-1");
        assertThat(response.getItems().get(1).getAuthor()).isNull();
    }

    @Test
    void createGraph_mapsCreatedGraphWithCurrentUser() {
        profileGrpcService.putProfile(TestData.profile(CURRENT_USER_ID, "current@example.com"));

        Graph created = controller().createGraph(TestData.graphInput("graph-new"));

        assertThat(created.getId()).isEqualTo("graph-new");
        assertThat(created.getName()).isEqualTo("graph input graph-new");
        assertThat(created.getVertexList()).hasSize(1);
        assertThat(created.getVertexList().get(0).getXCoordinate()).isEqualTo(1);
        assertThat(created.getVertexList().get(0).getYCoordinate()).isEqualTo(2);
        assertThat(created.getAuthor()).isNotNull();
        assertThat(created.getAuthor().getId()).isEqualTo(CURRENT_USER_ID);
    }

    @Test
    void generateGraph_mapsGeneratedGraphWithAuthor() {
        graphGrpcService.putGraph(TestData.graph("graph-gen", "author-1"));
        profileGrpcService.putProfile(TestData.profile("author-1", "author@example.com"));

        var request = new GenerateGraphRequest();
        request.setVertexCount(3);
        request.setEdgeCount(2);
        request.setIsDirect(true);
        request.setIsSaved(true);

        Graph generated = controller().generateGraph(request);

        assertThat(generated.getId()).isEqualTo("graph-gen");
        assertThat(generated.getAuthor().getId()).isEqualTo("author-1");
    }

    @Test
    void deleteGraph_returnsDeletedId() {
        graphGrpcService.putGraph(TestData.graph("graph-1", "author-1"));

        assertThat(controller().deleteGraph("graph-1")).isEqualTo("graph-1");
        assertThat(graphGrpcService.getGraphsByIds(java.util.List.of("graph-1"))).isEmpty();
    }
}

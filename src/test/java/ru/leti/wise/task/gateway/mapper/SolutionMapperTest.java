package ru.leti.wise.task.gateway.mapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.leti.graphql.types.GraphInput;
import ru.leti.graphql.types.SolutionGraphInput;
import ru.leti.graphql.types.VertexInput;
import ru.leti.graphql.types.EdgeInput;
import ru.leti.graphql.types.Color;
import ru.leti.wise.task.task.TaskOuterClass;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Решение проходит в task-сервис через {@code SolutionMapper}; здесь проверяется,
 * что списки вершин и рёбер не теряются при маппинге (регрессия на qualifiedByName
 * для вложенного графа).
 */
class SolutionMapperTest {

    private AnnotationConfigApplicationContext context;
    private SolutionMapper mapper;

    @BeforeEach
    void setUpMappers() {
        context = new AnnotationConfigApplicationContext();
        context.register(
                GraphMapperImpl.class, PluginMapperImpl.class, ProfileMapperImpl.class,
                SolutionMapperImpl.class, TaskMapperImpl.class, PaginationMapperImpl.class
        );
        context.refresh();
        mapper = context.getBean(SolutionMapperImpl.class);
    }

    @AfterEach
    void tearDownMappers() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void toSolutionGraphKeepsVertexAndEdgeLists() {
        var solution = solutionGraphInput("solution-1", "task-1", "author-1");

        TaskOuterClass.Solution grpcSolution = mapper.toSolutionGraph(solution, "author-1");

        assertThat(grpcSolution.getAuthorId()).isEqualTo("author-1");
        assertThat(grpcSolution.getTaskId()).isEqualTo("task-1");
        var graph = grpcSolution.getSolutionGraph().getGraph();
        assertThat(graph.getId()).isEqualTo("graph-1");
        assertThat(graph.getVertexListList()).hasSize(2);
        assertThat(graph.getVertexListList().get(0).getId()).isEqualTo(1);
        assertThat(graph.getVertexListList().get(0).getWeight()).isEqualTo(5);
        assertThat(graph.getVertexListList().get(0).getLabel()).isEqualTo("v1");
        assertThat(graph.getVertexListList().get(0).getXCoordinate()).isEqualTo(10);
        assertThat(graph.getVertexListList().get(0).getYCoordinate()).isEqualTo(20);
        assertThat(graph.getVertexListList().get(0).getColor()).isEqualTo(ru.leti.wise.task.graph.GraphOuterClass.Color.RED);
        assertThat(graph.getEdgeListList()).hasSize(1);
        assertThat(graph.getEdgeListList().get(0).getSource()).isEqualTo(1);
        assertThat(graph.getEdgeListList().get(0).getTarget()).isEqualTo(2);
        assertThat(graph.getEdgeListList().get(0).getWeight()).isEqualTo(7);
    }

    @Test
    void toSolutionGraphWithEmptyGraphProducesEmptyLists() {
        var solution = new SolutionGraphInput();
        solution.setTaskId("task-1");
        solution.setGraph(new GraphInput());

        TaskOuterClass.Solution grpcSolution = mapper.toSolutionGraph(solution, "author-1");

        var graph = grpcSolution.getSolutionGraph().getGraph();
        assertThat(graph.getVertexListList()).isEmpty();
        assertThat(graph.getEdgeListList()).isEmpty();
    }

    private SolutionGraphInput solutionGraphInput(String id, String taskId, String authorId) {
        var v1 = new VertexInput();
        v1.setId(1);
        v1.setWeight(5);
        v1.setLabel("v1");
        v1.setXCoordinate(10);
        v1.setYCoordinate(20);
        v1.setColor(Color.RED);
        var v2 = new VertexInput();
        v2.setId(2);
        v2.setWeight(6);
        v2.setLabel("v2");

        var edge = new EdgeInput();
        edge.setSource(1);
        edge.setTarget(2);
        edge.setWeight(7);
        edge.setColor(Color.BLUE);

        var graph = new GraphInput();
        graph.setId("graph-1");
        graph.setVertexCount(2);
        graph.setEdgeCount(1);
        graph.setIsDirect(false);
        graph.setVertexList(List.of(v1, v2));
        graph.setEdgeList(List.of(edge));

        var solution = new SolutionGraphInput();
        solution.setId(id);
        solution.setTaskId(taskId);
        solution.setAuthorId(authorId);
        solution.setGraph(graph);
        return solution;
    }
}

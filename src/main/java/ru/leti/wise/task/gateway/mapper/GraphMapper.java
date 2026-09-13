package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.*;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.graph.GraphGrpc;
import ru.leti.wise.task.graph.GraphOuterClass;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface GraphMapper {
    @Mapping(target = "authorId", expression = "java(authorId)")
    GraphOuterClass.Graph toGraph(GraphInput graph, @Context String authorId);

    GraphOuterClass.Vertex toVertex(VertexInput vertex);

    GraphOuterClass.Edge toEdge(EdgeInput edge);

    Graph toGraph(GraphOuterClass.Graph graph);

    List<Graph> toGraphs(List<GraphOuterClass.Graph> graphs);

    Vertex toVertex(GraphOuterClass.Vertex vertex);

    Edge toEdge(GraphOuterClass.Edge edge);

    GraphGrpc.GenerateGraphRequest toGenerateGraphRequest(GenerateGraphRequest generateGraphRequest);

    default GraphOuterClass.Color toColor(Color color) {
        return GraphOuterClass.Color.valueOf(color.name());
    }

    default Color toColor(GraphOuterClass.Color color) {
        return Color.valueOf(color.name());
    }
}

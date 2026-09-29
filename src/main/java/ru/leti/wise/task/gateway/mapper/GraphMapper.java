package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.*;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.graph.GraphGrpc;
import ru.leti.wise.task.graph.GraphGrpc.GraphFilter;
import ru.leti.wise.task.graph.GraphOuterClass;
import ru.leti.wise.task.profile.ProfileOuterClass;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {ProfileMapper.class})
public interface GraphMapper {

    GraphFilter toGraphFilter(GraphFilterInput filter);

    GraphGrpc.GetAllGraphsRequest toGetAllRequest(GetAllGraphsRequestInput request);

    @Mapping(target = "id", source = "graph.id")
    @Mapping(target = "author", source = "profile")
    @Mapping(target = "vertexList", source = "graph.vertexListList")
    @Mapping(target = "edgeList", source = "graph.edgeListList")
    Graph toGraph(GraphOuterClass.Graph graph, ProfileOuterClass.Profile profile);

    @Named("graphWithAuthor")
    @Mapping(target = "authorId", expression = "java(authorId)")
    @Mapping(target = "vertexListList", source = "vertexList")
    @Mapping(target = "edgeListList", source = "edgeList")
    GraphOuterClass.Graph toGraph(GraphInput graph, @Context String authorId);

    @Named("graphWithoutAuthor")
    @Mapping(target = "vertexListList", source = "vertexList")
    @Mapping(target = "edgeListList", source = "edgeList")
    GraphOuterClass.Graph toGraph(GraphInput graph);

    GraphOuterClass.Vertex toVertex(VertexInput vertex);

    GraphOuterClass.Edge toEdge(EdgeInput edge);

    @Mapping(target = "vertexList", source = "vertexListList")
    @Mapping(target = "edgeList", source = "edgeListList")
    Graph toGraph(GraphOuterClass.Graph graph);

    @Mapping(target = "xCoordinate", source = "XCoordinate")
    @Mapping(target = "yCoordinate", source = "YCoordinate")
    Vertex toVertex(GraphOuterClass.Vertex vertex);

    Edge toEdge(GraphOuterClass.Edge edge);

    @Mapping(target = "authorId", expression = "java(authorId)")
    GraphGrpc.GenerateGraphRequest toGenerateGraphRequest(
            GenerateGraphRequest req, @Context String authorId);
    default GraphOuterClass.Color toColor(Color color) {
        return GraphOuterClass.Color.valueOf(color.name());
    }

    default Color toColor(GraphOuterClass.Color color) {
        return Color.valueOf(color.name());
    }
}

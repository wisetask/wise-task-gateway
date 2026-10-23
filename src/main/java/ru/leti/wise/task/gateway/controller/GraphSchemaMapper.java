package ru.leti.wise.task.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.Graph;
import ru.leti.graphql.types.Profile;
import ru.leti.wise.task.gateway.service.ProfileService;
import ru.leti.wise.task.gateway.service.grpc.GraphGrpcService;

@Controller
@RequiredArgsConstructor
public class GraphSchemaMapper {
    private final GraphGrpcService graphGrpcService;
    private final ProfileService profileService;

    @SchemaMapping(typeName = "Graph", field = "author")
    public Profile getGraph(Graph graph) {
        try {
            var graphGrpc = graphGrpcService.getGraphById(graph.getId());
            return profileService.getProfile(graphGrpc.getAuthorId());
        } catch (Exception e) {
            return null;
        }
    }
}

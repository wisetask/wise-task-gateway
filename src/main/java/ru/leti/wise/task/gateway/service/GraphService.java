package ru.leti.wise.task.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.GraphMapper;
import ru.leti.wise.task.gateway.mapper.PaginationMapper;
import ru.leti.wise.task.gateway.service.grpc.graph.GraphGrpcService;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;
import ru.leti.wise.task.graph.GraphOuterClass;
import ru.leti.wise.task.profile.ProfileOuterClass;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class GraphService {

    private final GraphMapper graphMapper;
    private final GraphGrpcService graphGrpcService;
    private final ProfileGrpcService profileGrpcService;
    private final PaginationMapper paginationMapper;

    public GetAllGraphsResponse getAllGraphsResponse(GetAllGraphsRequestInput request) {
        log.debug("Запрос на получение списка графов {}", request);
        var grpcRequest = graphMapper.toGetAllRequest(request);
        var grpcResponse = graphGrpcService.getAllGraphs(grpcRequest);
        var items = toGraphs(grpcResponse.getItemsList());
        log.info("Запрос на получение графов от пользователя");
        var pagination = paginationMapper.toPagination(grpcResponse.getPagination());
        return GetAllGraphsResponse.newBuilder().items(items).pagination(pagination).build();
    }

    public Graph getGraphById(String id) {
        return toGraph(graphGrpcService.getGraphById(id));
    }

    public Graph createGraph(GraphInput graph, String authorId) {
        return toGraph(graphGrpcService.createGraph(graphMapper.toGraph(graph, authorId)));
    }

    public Graph generateGraph(GenerateGraphRequest generateGraphRequest) {
        return toGraph(graphGrpcService.generateRandomGraph(graphMapper.toGenerateGraphRequest(generateGraphRequest)));
    }

    public String deleteGraph(String id) {
        return graphGrpcService.deleteGraph(id);
    }

    public boolean isOwnerGraph(String userId, String graphId) {
        return graphGrpcService.getGraphById(graphId).getAuthorId().equals(userId);
    }

    private Graph toGraph(GraphOuterClass.Graph grpcGraph) {
        return graphMapper.toGraph(grpcGraph, getGrpcProfile(grpcGraph.getAuthorId()));
    }

    private List<Graph> toGraphs(List<GraphOuterClass.Graph> grpcGraphs) {
        var profiles = getProfilesByIds(grpcGraphs.stream().map(GraphOuterClass.Graph::getAuthorId).toList());

        return grpcGraphs.stream()
                .map(grpcGraph -> graphMapper.toGraph(grpcGraph, profiles.get(grpcGraph.getAuthorId())))
                .toList();
    }

    private ProfileOuterClass.Profile getGrpcProfile(String profileId) {
        return profileGrpcService.getProfile(profileId);
    }

    private Map<String, ProfileOuterClass.Profile> getProfilesByIds(List<String> profileIds) {
        var ids = profileIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        return profileGrpcService.getProfilesByIds(ids).stream()
                .collect(Collectors.toMap(
                        ProfileOuterClass.Profile::getId,
                        profile -> profile
                ));
    }
}
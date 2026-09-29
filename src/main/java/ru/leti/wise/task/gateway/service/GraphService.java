package ru.leti.wise.task.gateway.service;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
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
    @Lazy @Autowired private GraphService self;

    public GetAllGraphsResponse getAllGraphsResponse(GetAllGraphsRequestInput request) {
        log.debug("getAllGraphs request, {}", request);
        var grpcRequest = graphMapper.toGetAllRequest(request);
        var grpcResponse = graphGrpcService.getAllGraphs(grpcRequest);
        log.info("getAllGraphs request, fetched {} graphs", grpcResponse.getItemsCount());
        var items = toGraphs(grpcResponse.getItemsList());
        var pagination = paginationMapper.toPagination(grpcResponse.getPagination());
        return GetAllGraphsResponse.newBuilder().items(items).pagination(pagination).build();
    }

    public Graph getGraphById(String id) {
        log.debug("getGraphById request, {}", id);
        var graph = self.toGraph(graphGrpcService.getGraphById(id));
        log.info("getGraphById request, mapped graph {}", id);
        return graph;
    }

    public Map<String, Graph> getGraphsByIds(List<String> graphIds) {
        var ids = graphIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        log.debug("getGraphsByIds request, fetching graphs {}", ids);
        var graphs = graphGrpcService.getGraphsByIds(ids);
        var profiles = getProfilesByIds(graphs.stream().map(GraphOuterClass.Graph::getAuthorId).toList());
        log.debug("getGraphsByIds request, fetching authors for {} graphs", graphs.size());

        return graphs.stream()
                .collect(Collectors.toMap(
                        GraphOuterClass.Graph::getId,
                        graph -> graphMapper.toGraph(graph, profiles.get(graph.getAuthorId()))
                ));
    }

    @CachePut(value = "graph", key = "#result.id", cacheManager = "localCacheManager")
    public Graph createGraph(GraphInput graph, String authorId) {
        log.debug("createGraph request, {}", graph);
        var created = toGraph(graphGrpcService.createGraph(graphMapper.toGraph(graph, authorId)));
        log.info("createGraph request, created graph {}", created.getId());
        return created;
    }

    @CachePut(value = "graph", key = "#result.id", cacheManager = "localCacheManager")
    public Graph generateGraph(GenerateGraphRequest generateGraphRequest, String authorId) {
        log.debug("generateGraph request, {}", generateGraphRequest);
        var generated = toGraph(graphGrpcService.generateRandomGraph(
                graphMapper.toGenerateGraphRequest(generateGraphRequest, authorId))
        );
        log.info("generateGraph request, generated graph {}", generated.getId());
        return generated;
    }

    @CacheEvict(value = "graph", key = "#id", cacheManager = "localCacheManager")
    public String deleteGraph(String id) {
        log.debug("deleteGraph request, {}", id);
        graphGrpcService.deleteGraph(id);
        log.info("deleteGraph request, deleted graph {}", id);
        return id;
    }

    public boolean isOwnerGraph(String userId, String graphId) {
        log.debug("isOwnerGraph request, graph {} owner {}", graphId, userId);
        return graphGrpcService.getGraphById(graphId).getAuthorId().equals(userId);
    }

    @Cacheable(value = "graph", key = "#grpcGraph.id", cacheManager = "localCacheManager")
    public Graph toGraph(GraphOuterClass.Graph grpcGraph) {
        log.debug("toGraph mapping, mapping graph {} with author", grpcGraph.getId());
        return graphMapper.toGraph(grpcGraph, getGrpcProfile(grpcGraph.getAuthorId()));
    }

    private List<Graph> toGraphs(List<GraphOuterClass.Graph> grpcGraphs) {
        var profiles = getProfilesByIds(grpcGraphs.stream().map(GraphOuterClass.Graph::getAuthorId).toList());
        log.debug("getAllGraphs request, mapping {} graphs with authors", grpcGraphs.size());

        return grpcGraphs.stream()
                .map(grpcGraph -> graphMapper.toGraph(grpcGraph, profiles.get(grpcGraph.getAuthorId())))
                .toList();
    }

    private ProfileOuterClass.Profile getGrpcProfile(String profileId) {
        log.debug("mapping, fetching profile {}", profileId);
        try {
            return profileGrpcService.getProfile(profileId);
        } catch (StatusRuntimeException e) {
            log.warn("mapping, failed to fetch profile {}, returning null profile", profileId, e);
            return null;
        }
    }

    private Map<String, ProfileOuterClass.Profile> getProfilesByIds(List<String> profileIds) {
        var ids = profileIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        log.debug("mapping, fetching profiles {}", ids);
        try {
            return profileGrpcService.getProfilesByIds(ids).stream()
                    .collect(Collectors.toMap(
                            ProfileOuterClass.Profile::getId,
                            profile -> profile
                    ));
        } catch (StatusRuntimeException e) {
            log.warn("mapping, failed to fetch profiles {}, returning empty map", ids, e);
            return Map.of();
        }
    }
}
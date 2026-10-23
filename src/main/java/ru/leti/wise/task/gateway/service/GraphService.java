package ru.leti.wise.task.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.GraphMapper;
import ru.leti.wise.task.gateway.mapper.PaginationMapper;
import ru.leti.wise.task.gateway.service.grpc.GraphGrpcService;
import ru.leti.wise.task.graph.GraphOuterClass;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class GraphService {
    private final GraphMapper graphMapper;
    private final GraphGrpcService graphGrpcService;
    private final PaginationMapper paginationMapper;

    public GetAllGraphsResponse getAllGraphsResponse(GetAllGraphsRequestInput request) {
        log.debug("getAllGraphs request, {}", request);
        var grpcRequest = graphMapper.toGetAllRequest(request);
        var grpcResponse = graphGrpcService.getAllGraphs(grpcRequest);
        log.info("getAllGraphs request, fetched {} graphs", grpcResponse.getItemsCount());
        var items = toGraphs(grpcResponse.getItemsList());
        var pagination = paginationMapper.toPagination(grpcResponse.getPagination());
        return GetAllGraphsResponse.newBuilder().items(items).pagination(pagination).build();
    }

    @Cacheable(value = "graph", key = "#id", cacheManager = "localCacheManager")
    public Graph getGraphById(String id) {
        log.debug("getGraphById request, {}", id);
        var graphGrpc = graphGrpcService.getGraphById(id);
        var graph = toGraph(graphGrpc);
        log.info("getGraphById request, mapped graph {}", id);
        return graph;
    }

    @CachePut(value = "graph", key = "#result.id", cacheManager = "localCacheManager")
    public Graph createGraph(GraphInput graph, String authorId) {
        log.debug("createGraph request, {}", graph);
        var graphGrpc = graphGrpcService.createGraph(graphMapper.toGraph(graph, authorId));
        var created = toGraph(graphGrpc);
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

    public Graph toGraph(GraphOuterClass.Graph grpcGraph) {
        log.debug("toGraph mapping, mapping graph {}", grpcGraph.getId());
        return graphMapper.toGraph(grpcGraph);
    }

    private List<Graph> toGraphs(List<GraphOuterClass.Graph> grpcGraphs) {
        log.debug("getAllGraphs request, mapping {} graphs", grpcGraphs.size());
        return grpcGraphs.stream()
                .map(this::toGraph)
                .toList();
    }

    public Map<String, Graph> getGraphsByIds(List<String> graphIds) {
        log.debug("getGraphsById request, fetching {} graphs", graphIds.size());
        return graphGrpcService.getGraphsByIds(graphIds).stream()
                .map(this::toGraph)
                .collect(Collectors.toMap(Graph::getId, graph -> graph));
    }
}
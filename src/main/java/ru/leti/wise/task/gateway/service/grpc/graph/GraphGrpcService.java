package ru.leti.wise.task.gateway.service.grpc.graph;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.graph.GraphGrpc;
import ru.leti.wise.task.graph.GraphGrpc.GetAllGraphsRequest;
import ru.leti.wise.task.graph.GraphGrpc.GetAllGraphsResponse;
import ru.leti.wise.task.graph.GraphOuterClass;
import ru.leti.wise.task.graph.GraphServiceGrpc.GraphServiceBlockingStub;

import java.util.List;


@Slf4j
@Component
@Observed
@RequiredArgsConstructor
public class GraphGrpcService {

    private final GraphServiceBlockingStub graphService;

    public GraphOuterClass.Graph getGraphById(String id) {
        log.debug("GraphService.getGraphById request, graph id {}", id);
        var request = GraphGrpc.GetGraphByIdRequest.newBuilder()
                .setId(id)
                .build();

        var graph = graphService.getGraphById(request).getGraph();
        log.debug("GraphService.getGraphById request, fetched graph {}", graph.getId());
        return graph;
    }

    public List<GraphOuterClass.Graph> getGraphsByIds(List<String> graphIds) {
        log.debug("GraphService.getGraphsByIds request, graph ids {}", graphIds);
        var request = GraphGrpc.GraphIds.newBuilder()
                .addAllGraphIds(graphIds)
                .build();

        var graphs = graphService.getGraphsByIds(request).getGraphsList();
        log.debug("GraphService.getGraphsByIds request, fetched {} graphs", graphs.size());
        return graphs;
    }

    public GetAllGraphsResponse getAllGraphs(GetAllGraphsRequest request) {
        log.debug("GraphService.getAllGraphs request, {}", request);
        var response = graphService.getAllGraphs(request);
        log.debug("GraphService.getAllGraphs request, fetched {} graphs", response.getItemsCount());
        return response;
    }

    public String deleteGraph(String id) {
        log.debug("GraphService.deleteGraph request, graph id {}", id);
        var request = GraphGrpc.RemoveGraphRequest.newBuilder()
                .setId(id)
                .build();

        var deletedId = graphService.removeGraph(request).getId();
        log.debug("GraphService.deleteGraph request, deleted graph {}", deletedId);
        return deletedId;
    }

    public GraphOuterClass.Graph createGraph(GraphOuterClass.Graph graph) {
        log.debug("GraphService.createGraph request, graph id {}", graph.getId());
        var request = GraphGrpc.CreateGraphRequest.newBuilder()
                .setGraph(graph)
                .build();

        var created = graphService.createGraph(request).getGraph();
        log.debug("GraphService.createGraph request, created graph {}", created.getId());
        return created;
    }

    public GraphOuterClass.Graph generateRandomGraph(GraphGrpc.GenerateGraphRequest request) {
        log.debug("GraphService.generateRandomGraph request, {}", request);
        var graph = graphService.generateRandomGraph(request).getGraph();
        log.debug("GraphService.generateRandomGraph request, generated graph {}", graph.getId());
        return graph;
    }
}

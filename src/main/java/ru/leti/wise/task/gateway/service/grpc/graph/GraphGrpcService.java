package ru.leti.wise.task.gateway.service.grpc.graph;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.graph.GraphGrpc;
import ru.leti.wise.task.graph.GraphGrpc.GetAllGraphsRequest;
import ru.leti.wise.task.graph.GraphGrpc.GetAllGraphsResponse;
import ru.leti.wise.task.graph.GraphOuterClass;
import ru.leti.wise.task.graph.GraphServiceGrpc.GraphServiceBlockingStub;

import java.util.List;


@Component
@Observed
@RequiredArgsConstructor
public class GraphGrpcService {

    private final GraphServiceBlockingStub graphService;

    public GraphOuterClass.Graph getGraphById(String id) {
        var request = GraphGrpc.GetGraphByIdRequest.newBuilder()
                .setId(id)
                .build();

        return graphService.getGraphById(request).getGraph();
    }

    public List<GraphOuterClass.Graph> getGraphsByIds(List<String> graphIds) {
        var request = GraphGrpc.GraphIds.newBuilder()
                .addAllGraphIds(graphIds)
                .build();

        return graphService.getGraphsByIds(request).getGraphsList();
    }

    public GetAllGraphsResponse getAllGraphs(GetAllGraphsRequest request) {
        return graphService.getAllGraphs(request);
    }

    public String deleteGraph(String id) {
        var request = GraphGrpc.RemoveGraphRequest.newBuilder()
                .setId(id)
                .build();

        return graphService.removeGraph(request).getId();
    }

    public GraphOuterClass.Graph createGraph(GraphOuterClass.Graph graph) {
        var request = GraphGrpc.CreateGraphRequest.newBuilder()
                .setGraph(graph)
                .build();

        return graphService.createGraph(request).getGraph();
    }

    public GraphOuterClass.Graph generateRandomGraph(GraphGrpc.GenerateGraphRequest request) {
        return graphService.generateRandomGraph(request).getGraph();
    }
}
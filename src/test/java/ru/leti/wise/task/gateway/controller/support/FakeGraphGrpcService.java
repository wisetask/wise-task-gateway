package ru.leti.wise.task.gateway.controller.support;

import io.grpc.Status;
import ru.leti.wise.task.gateway.service.grpc.graph.GraphGrpcService;
import ru.leti.wise.task.graph.GraphGrpc.GenerateGraphRequest;
import ru.leti.wise.task.graph.GraphGrpc.GetAllGraphsRequest;
import ru.leti.wise.task.graph.GraphGrpc.GetAllGraphsResponse;
import ru.leti.wise.task.graph.GraphOuterClass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Фейковый graph-сервис: отдаёт заранее подготовленные графы, умеет имитировать gRPC-ошибки.
 */
public class FakeGraphGrpcService extends GraphGrpcService {

    private final Map<String, GraphOuterClass.Graph> graphs = new LinkedHashMap<>();
    private boolean failGetGraphsByIds;

    public FakeGraphGrpcService() {
        super(null);
    }

    public void putGraph(GraphOuterClass.Graph graph) {
        graphs.put(graph.getId(), graph);
    }

    public void failGetGraphsByIds() {
        failGetGraphsByIds = true;
    }

    @Override
    public GraphOuterClass.Graph getGraphById(String id) {
        var graph = graphs.get(id);
        if (graph == null) {
            throw Status.NOT_FOUND.withDescription("graph " + id + " not found").asRuntimeException();
        }
        return graph;
    }

    @Override
    public List<GraphOuterClass.Graph> getGraphsByIds(List<String> graphIds) {
        if (failGetGraphsByIds) {
            throw Status.UNAVAILABLE.withDescription("graph service unavailable").asRuntimeException();
        }
        return graphIds.stream().map(graphs::get).filter(Objects::nonNull).toList();
    }

    @Override
    public GetAllGraphsResponse getAllGraphs(GetAllGraphsRequest request) {
        return GetAllGraphsResponse.newBuilder()
                .addAllItems(graphs.values())
                .setPagination(TestData.pagination())
                .build();
    }

    @Override
    public String deleteGraph(String id) {
        graphs.remove(id);
        return id;
    }

    @Override
    public GraphOuterClass.Graph createGraph(GraphOuterClass.Graph graph) {
        graphs.put(graph.getId(), graph);
        return graph;
    }

    @Override
    public GraphOuterClass.Graph generateRandomGraph(GenerateGraphRequest request) {
        return graphs.values().stream()
                .findFirst()
                .orElseThrow(() -> Status.NOT_FOUND
                        .withDescription("no graphs configured")
                        .asRuntimeException());
    }
}

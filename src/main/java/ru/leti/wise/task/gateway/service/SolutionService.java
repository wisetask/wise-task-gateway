package ru.leti.wise.task.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.PaginationMapper;
import ru.leti.wise.task.gateway.mapper.SolutionMapper;
import ru.leti.wise.task.gateway.service.grpc.TaskGrpcService;
import ru.leti.wise.task.task.TaskOuterClass;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolutionService {
    private final SolutionMapper solutionMapper;
    private final PaginationMapper paginationMapper;
    private final TaskGrpcService taskGrpcService;

    public GetAllTaskSolutionsResponse getAllTaskSolutionsResponse(GetAllTaskSolutionsRequestInput request) {
        log.debug("getAllTaskSolutions request, {}", request);
        var grpcRequest = solutionMapper.toGetAllRequest(request);
        var grpcResponse = taskGrpcService.getAllTaskSolutions(grpcRequest);
        log.info("getAllTaskSolutions request, fetched {} task solutions", grpcResponse.getItemsCount());
        var items = toSolutions(grpcResponse.getItemsList());
        var pagination = paginationMapper.toPagination(grpcResponse.getPagination());
        return GetAllTaskSolutionsResponse.newBuilder().items(items).pagination(pagination).build();
    }

    @CachePut(value = "solution", key = "#result.id", cacheManager = "localCacheManager")
    public SolutionGraph solveTaskGraph(SolutionGraphInput solution, String authorId) {
        log.debug("solveTaskGraph request, {}", solution);
        var grpcSolution = taskGrpcService.solveTask(solutionMapper.toSolutionGraph(solution, authorId));
        var solved = (SolutionGraph) toSolution(grpcSolution);
        log.info("solveTaskGraph request, solved task graph {}", solved.getId());
        return solved;
    }

    @CachePut(value = "solution", key = "#result.id", cacheManager = "localCacheManager")
    public SolutionImplementation solveTaskImplementation(SolutionImplementationInput solution, String authorId) {
        log.debug("solveTaskImplementation request, {}", solution);
        var grpcSolution = taskGrpcService.solveTask(solutionMapper.toSolutionImplementation(solution, authorId));
        var solved = (SolutionImplementation) toSolution(grpcSolution);
        log.info("solveTaskImplementation request, solved task implementation {}", solved.getId());
        return solved;
    }

    private List<Solution> toSolutions(List<TaskOuterClass.Solution> grpcSolutions) {
        log.debug("getAllTaskSolutions request, mapping {} solutions with tasks and authors", grpcSolutions.size());
        return grpcSolutions.stream()
                .map(this::toSolution)
                .toList();
    }

    public Solution toSolution(TaskOuterClass.Solution grpcSolution) {
        log.debug("toSolution mapping, mapping solution {}", grpcSolution.getId());
        return solutionMapper.toSolution(grpcSolution);
    }

    @Cacheable(value = "solution", key = "#id", cacheManager = "localCacheManager")
    public Solution getTaskSolution(String id) {
        log.debug("getTaskSolution request, {}", id);
        var solutionGrpc = taskGrpcService.getTaskSolution(id);
        return toSolution(solutionGrpc);
    }
}

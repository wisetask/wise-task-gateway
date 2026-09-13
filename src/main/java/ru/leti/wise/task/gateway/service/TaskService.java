package ru.leti.wise.task.gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.GraphMapper;
import ru.leti.wise.task.gateway.mapper.SolutionMapper;
import ru.leti.wise.task.gateway.service.grpc.graph.GraphGrpcService;
import ru.leti.wise.task.task.TaskOuterClass;

@Component
@RequiredArgsConstructor
public class TaskService {

    private final GraphGrpcService graphGrpcService;
    private final GraphMapper graphMapper;
    private final SolutionMapper solutionMapper;

    public Solution buildSolutionWithGraph(TaskOuterClass.Solution solution) {
//        graphGrpcService.getGraphById(solution.getSolutionGraph().getGraph().getId());
        return solutionMapper.toSolution(solution);
    }
}

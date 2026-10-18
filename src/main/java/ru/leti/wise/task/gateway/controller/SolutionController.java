package ru.leti.wise.task.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.metrics.GraphQlMetrics;
import ru.leti.wise.task.gateway.service.SolutionService;
import ru.leti.wise.task.gateway.utils.SecurityUtils;

@Controller
@RequiredArgsConstructor
public class SolutionController {
    private final SolutionService solutionService;
    private final GraphQlMetrics metrics;

    @QueryMapping
    @PreAuthorize("""
            authentication.principal.id.equals(#request.authorId) or
            hasRole("AUTHOR") and
            @taskGrpcService.getTask(#request.taskId).getAuthorId().equals(authentication.principal.id) or
            hasRole("ADMIN")
            """)
    public GetAllTaskSolutionsResponse getAllTaskSolutions(@Argument GetAllTaskSolutionsRequestInput request) {
        metrics.increment("getAllTaskSolutions");
        return solutionService.getAllTaskSolutionsResponse(request);
    }

    @QueryMapping
    @PreAuthorize("""
            @taskGrpcService.getTaskSolution(#id).getAuthorId().equals(authentication.principal.id) or
            hasRole("AUTHOR") and
            @taskGrpcService.getTask(@taskGrpcService.getTaskSolution(#id).getTaskId())
                    .getAuthorId().equals(authentication.principal.id) or
            hasRole("ADMIN")
            """)
    public Solution getTaskSolution(@Argument String id) {
        metrics.increment("getTaskSolution");
        return solutionService.getTaskSolution(id);
    }

    @MutationMapping
    @PreAuthorize("""
            hasAnyRole("USER", "AUTHOR", "ADMIN")
            """)
    public SolutionGraph solveTaskGraph(@Argument SolutionGraphInput solution) {
        metrics.increment("solveTaskGraph");
        return solutionService.solveTaskGraph(solution, SecurityUtils.getUserId());
    }

    @MutationMapping
    @PreAuthorize("""
            hasAnyRole("USER", "AUTHOR", "ADMIN")
            """)
    public SolutionImplementation solveTaskImplementation(@Argument SolutionImplementationInput solution) {
        metrics.increment("solveTaskImplementation");
        return solutionService.solveTaskImplementation(solution, SecurityUtils.getUserId());
    }
}

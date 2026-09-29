package ru.leti.wise.task.gateway.controller;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.service.TaskService;
import ru.leti.wise.task.gateway.service.grpc.task.TaskGrpcService;
import ru.leti.wise.task.gateway.utils.SecurityUtils;

@Observed
@Controller
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;


    @MutationMapping
    @PreAuthorize(
            "hasRole(\"AUTHOR\") and " +
            "@taskGrpcService.getTask(#id).getAuthorId().equals(authentication.principal.id) or" +
            " hasRole(\"ADMIN\")")
    public String deleteTask(@Argument String id) {
        return taskService.deleteTask(id);
    }


    @QueryMapping
    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\",\"ADMIN\")")
    public GetAllTaskSolutionsResponse getAllTaskSolutions(@Argument GetAllTaskSolutionsRequestInput request) {
        return taskService.getAllTaskSolutionsResponse(request);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\",\"ADMIN\")")
    public GetAllTaskResponse getAllTasks(@Argument GetAllTaskRequestInput request) {
        return taskService.getAllTaskResponse(request);
    }


    @QueryMapping
    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\",\"ADMIN\")")
    public Task getTask(@Argument String id) {
        return taskService.getTask(id);
    }

    @QueryMapping
    @PreAuthorize(value = "hasRole(\"USER\") and " +
            "@taskGrpcService.getTaskSolution(#id).getAuthorId().equals(authentication.principal.id)" +
            " or hasRole(\"AUTHOR\") and " +
            "@taskGrpcService.getTask(taskGrpcService.getTaskSolution(#id).getTaskId())" +
            ".getAuthorId().equals(authentication.principal.id) or" +
            " hasRole(\"ADMIN\")")
    public Solution getTaskSolution(@Argument String id) {
        return taskService.getTaskSolution(id);
    }


    @MutationMapping
    @PreAuthorize("hasAnyRole(\"AUTHOR\",\"ADMIN\")")
    public TaskGraph createTaskGraph(@Argument TaskGraphInput task) {
        return taskService.createTaskGraph(task, SecurityUtils.getUserId());
    }


    @MutationMapping
    @PreAuthorize("hasAnyRole(\"AUTHOR\",\"ADMIN\")")
    public TaskImplementation createTaskImplementation(@Argument TaskImplementationInput task) {
        return taskService.createTaskImplementation(task, SecurityUtils.getUserId());
    }


    @MutationMapping
    @PreAuthorize("hasRole(\"AUTHOR\") " +
            "and @taskGrpcService.getTask(#task.getId()).getAuthorId().equals(authentication.principal.id) " +
            "or hasRole(\"ADMIN\")")
    public TaskGraph updateTaskGraph(@Argument TaskGraphInput task) {
        return taskService.updateTaskGraph(task, SecurityUtils.getUserId());
    }

    @MutationMapping
    @PreAuthorize("hasRole(\"AUTHOR\") " +
            "and @taskGrpcService.getTask(#task.getId()).getAuthorId().equals(authentication.principal.id) " +
            "or hasRole(\"ADMIN\")")
    public TaskImplementation updateTaskImplementation(@Argument TaskImplementationInput task) {
        return taskService.updateTaskImplementation(task, SecurityUtils.getUserId());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\",\"ADMIN\")")
    public SolutionGraph solveTaskGraph(@Argument SolutionGraphInput solution) {
        return taskService.solveTaskGraph(solution, SecurityUtils.getUserId());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\",\"ADMIN\")")
    public SolutionImplementation solveTaskImplementation(@Argument SolutionImplementationInput solution) {
        return taskService.solveTaskImplementation(solution, SecurityUtils.getUserId());
    }
}

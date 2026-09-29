package ru.leti.wise.task.gateway.controller;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.dto.UserCredentials;
import ru.leti.wise.task.gateway.mapper.SolutionMapper;
import ru.leti.wise.task.gateway.mapper.TaskMapper;
import ru.leti.wise.task.gateway.service.grpc.task.TaskGrpcService;

import java.util.List;

@Observed
@Controller
@RequiredArgsConstructor
public class TaskController {

    private final TaskMapper taskMapper;
    private final SolutionMapper solutionMapper;
    private final TaskGrpcService taskGrpcService;


    @MutationMapping
    @PreAuthorize(
            "hasRole(\"AUTHOR\") and " +
                    "@taskGrpcService.getTask(#id).getAuthorId().equals(authentication.principal.profile.id) or" +
                    " hasRole(\"ADMIN\")")
    public String deleteTask(@Argument String id) {
        taskGrpcService.deleteTask(id);
        return id;
    }


    @QueryMapping
    @PreAuthorize(value = "hasRole(\"AUTHOR\") and " +
            "@taskGrpcService.getTask(#taskId).getAuthorId().equals(authentication.principal.profile.id) or" +
            " hasRole(\"ADMIN\")")
    public List<Solution> getAllTaskSolutions(@Argument String userId, @Argument String taskId) {
        return solutionMapper.toSolutions(taskGrpcService.getAllTaskSolutions(taskId, userId));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\",\"ADMIN\")")
    public List<Task> getAllTasks() {
        return taskMapper.toTasks(taskGrpcService.getAllTasks());
    }


    @QueryMapping
    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\",\"ADMIN\")")
    public Task getTask(@Argument String id) {
        var taskResponse = taskGrpcService.getTask(id);
        return taskMapper.toTaskGraph(taskResponse);
    }

    @QueryMapping
    @PreAuthorize(value = "hasRole(\"USER\") and " +
            "@taskGrpcService.getTaskSolution(#id).getAuthorId().equals(authentication.principal.id)" +
            " or hasRole(\"AUTHOR\") and " +
            "@taskGrpcService.getTask(taskGrpcService.getTaskSolution(#id).getTaskId())" +
            ".getAuthorId().equals(authentication.principal.id) or" +
            " hasRole(\"ADMIN\")")
    public Solution getTaskSolution(@Argument String id) {
        var solution = taskGrpcService.getTaskSolution(id);
        return solutionMapper.toSolution(solution);
    }


    @QueryMapping
    @PreAuthorize("hasAnyRole(\"AUTHOR\",\"ADMIN\")")
    public List<Solution> getUserSolutionStatistic(@Argument String userId) {
        return solutionMapper.toSolutions(taskGrpcService.getUserSolutionStatistic(userId));
    }


    @MutationMapping
    @PreAuthorize("hasAnyRole(\"AUTHOR\",\"ADMIN\")")
    public TaskGraph createTaskGraph(
            @Argument TaskGraphInput task,
            @AuthenticationPrincipal UserCredentials user
    ) {
        var taskGrpc = taskGrpcService.createTask(taskMapper.toTaskGraph(task, user.getId()));
        return taskMapper.toTaskGraph(taskGrpc);
    }


    @MutationMapping
    @PreAuthorize("hasAnyRole(\"AUTHOR\",\"ADMIN\")")
    public TaskImplementation createTaskImplementation(
            @Argument TaskImplementationInput task,
            @AuthenticationPrincipal UserCredentials user
    ) {
        var taskGrpc = taskGrpcService.createTask(taskMapper.toTaskImplementation(task, user.getId()));
        return taskMapper.toTaskImplementation(taskGrpc);
    }


    @MutationMapping
    @PreAuthorize("hasRole(\"AUTHOR\") " +
            "and @taskGrpcService.getTask(#task.getId()).getAuthorId().equals(authentication.principal.id) " +
            "or hasRole(\"ADMIN\")")
    public TaskGraph updateTaskGraph(
            @Argument TaskGraphInput task,
            @AuthenticationPrincipal UserCredentials user
    ) {
        var taskGrpc = taskGrpcService.updateTask(
                taskMapper.toTaskGraph(task, user.getId()));
        return taskMapper.toTaskGraph(
                taskGrpc
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole(\"AUTHOR\") " +
            "and @taskGrpcService.getTask(#task.getId()).getAuthorId().equals(authentication.principal.id) " + //TODO вынести в бизнес логику
            "or hasRole(\"ADMIN\")")
    public TaskImplementation updateTaskImplementation(
            @Argument TaskImplementationInput task,
            @AuthenticationPrincipal UserCredentials user
    ) {
        var taskGrpc = taskGrpcService.updateTask(
                taskMapper.toTaskImplementation(task, user.getId())
        );
        return taskMapper.toTaskImplementation(taskGrpc);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\",\"ADMIN\")")
    public SolutionGraph solveTaskGraph(
            @Argument SolutionGraphInput solution,
            @AuthenticationPrincipal UserCredentials user
    ) {
        var solutionGrpc = taskGrpcService.solveTask(solutionMapper.toSolutionGraph(solution, user.getId()));
        return solutionMapper.toSolutionGraph(solutionGrpc);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\",\"ADMIN\")")
    public SolutionImplementation solveTaskImplementation(
            @Argument SolutionImplementationInput solution,
            @AuthenticationPrincipal UserCredentials user
    ) {
        var solutionGrpc = taskGrpcService.solveTask(solutionMapper.toSolutionImplementation(solution, user.getId()));
        return solutionMapper.toSolutionImplementation(solutionGrpc);
    }
}

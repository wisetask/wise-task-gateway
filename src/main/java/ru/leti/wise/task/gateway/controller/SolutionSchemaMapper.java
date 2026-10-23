package ru.leti.wise.task.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.service.PluginService;
import ru.leti.wise.task.gateway.service.ProfileService;
import ru.leti.wise.task.gateway.service.TaskService;
import ru.leti.wise.task.gateway.service.grpc.TaskGrpcService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SolutionSchemaMapper {
    private final TaskGrpcService taskGrpcService;
    private final TaskService taskService;
    private final ProfileService profileService;
    private final PluginService pluginService;

    @SchemaMapping(typeName = "SolutionGraph", field = "task")
    public Task getTask(SolutionGraph solution) {
        try {
            var solutionGrpc = taskGrpcService.getTaskSolution(solution.getId());
            return taskService.getTask(solutionGrpc.getTaskId());
        } catch (Exception e) {
            return null;
        }
    }

    @SchemaMapping(typeName = "SolutionImplementation", field = "task")
    public Task getTask(SolutionImplementation solution) {
        try {
            var solutionGrpc = taskGrpcService.getTaskSolution(solution.getId());
            return taskService.getTask(solutionGrpc.getTaskId());
        } catch (Exception e) {
            return null;
        }
    }

    @SchemaMapping(typeName = "SolutionGraph", field = "author")
    public Profile getAuthor(SolutionGraph solution) {
        try {
            var solutionGrpc = taskGrpcService.getTaskSolution(solution.getId());
            return profileService.getProfile(solutionGrpc.getAuthorId());
        } catch (Exception e) {
            return null;
        }
    }

    @SchemaMapping(typeName = "SolutionImplementation", field = "author")
    public Profile getAuthor(SolutionImplementation solution) {
        try {
            var solutionGrpc = taskGrpcService.getTaskSolution(solution.getId());
            return profileService.getProfile(solutionGrpc.getAuthorId());
        } catch (Exception e) {
            return null;
        }
    }

    @SchemaMapping(typeName = "SolutionGraph", field = "pluginResults")
    public List<PluginResult> getPluginResults(SolutionGraph solution) {
        try {
            var solutionGrpc = taskGrpcService.getTaskSolution(solution.getId()).getSolutionGraph();
            return solutionGrpc.getPluginResultsList().stream()
                    .map(result -> new PluginResult(
                            pluginService.getPlugin(result.getPluginId()),
                            result.getIsCorrect(),
                            result.getValue(),
                            result.getTrueValue(),
                            result.getPluginMessage()
                    )).toList();
        } catch (Exception e) {
            return null;
        }
    }

}

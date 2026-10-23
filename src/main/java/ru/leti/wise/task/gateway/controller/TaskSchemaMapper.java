package ru.leti.wise.task.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.service.PluginService;
import ru.leti.wise.task.gateway.service.ProfileService;
import ru.leti.wise.task.gateway.service.grpc.TaskGrpcService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TaskSchemaMapper {
    private final TaskGrpcService taskGrpcService;
    private final ProfileService profileService;
    private final PluginService pluginService;

    @SchemaMapping(typeName = "TaskGraph", field = "author")
    public Profile getProfile(TaskGraph task) {
        try {
            var taskGrpc = taskGrpcService.getTask(task.getId());
            return profileService.getProfile(taskGrpc.getAuthorId());
        } catch (Exception e) {
            return null;
        }
    }

    @SchemaMapping(typeName = "TaskImplementation", field = "author")
    public Profile getProfile(TaskImplementation task) {
        try {
            var taskGrpc = taskGrpcService.getTask(task.getId());
            return profileService.getProfile(taskGrpc.getAuthorId());
        } catch (Exception e) {
            return null;
        }
    }

    @SchemaMapping(typeName = "TaskGraph", field = "conditions")
    public List<PluginInfo> getConditions(TaskGraph task) {
        try {
            var taskGrpc = taskGrpcService.getTask(task.getId());
            return taskGrpc.getTaskGraph().getConditionList().stream().map(condition ->
                    new PluginInfo(
                            pluginService.getPlugin(condition.getPluginId()),
                            condition.getValue(),
                            condition.getMistakeText(),
                            condition.getSign(),
                            PluginType.valueOf(condition.getPluginType().name())
                    )
            ).toList();
        } catch (Exception e) {
            return null;
        }
    }

    @SchemaMapping(typeName = "TaskImplementation", field = "plugin")
    public Plugin getPluginForTaskImpl(TaskImplementation task) {
        try {
            var taskGrpc = taskGrpcService.getTask(task.getId()).getTaskImplementation();
            return pluginService.getPlugin(taskGrpc.getPluginId());
        } catch (Exception e) {
            return null;
        }
    }
}

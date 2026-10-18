package ru.leti.wise.task.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.service.ProfileService;
import ru.leti.wise.task.gateway.service.grpc.PluginGrpcService;

@Controller
@RequiredArgsConstructor
public class PluginSchemaMapper {
    private final PluginGrpcService pluginGrpcService;
    private final ProfileService profileService;

    @SchemaMapping(typeName = "Plugin", field = "author")
    public Profile getProfile(Plugin plugin) {
        try {
            var taskGrpc = pluginGrpcService.getPlugin(plugin.getId());
            return profileService.getProfile(taskGrpc.getAuthorId());
        } catch (Exception e) {
            return null;
        }
    }
}

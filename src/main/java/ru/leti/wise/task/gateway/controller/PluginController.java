package ru.leti.wise.task.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;

import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.PluginMapper;
import ru.leti.wise.task.gateway.service.grpc.plugin.PluginGrpcService;

import java.util.List;


@Controller
@RequiredArgsConstructor
public class PluginController {

    private final PluginGrpcService pluginGrpcService;
    private final PluginMapper pluginMapper;

    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\", \"ADMIN\")")
    @MutationMapping
    public ImplementationResult checkPluginImplementation(@Argument String id, @Argument String file) {
        return pluginMapper.toImplementationResult(pluginGrpcService.checkPluginImplementation(id, file));
    }

    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\", \"ADMIN\")")
    @MutationMapping
    public String checkPluginSolution(@Argument SolutionInput solution) {
        return pluginGrpcService.checkPluginSolution(solution);
    }

    @PreAuthorize("hasAnyRole(\"AUTHOR\",\"ADMIN\")")
    @MutationMapping
    public Plugin createPlugin(@Argument PluginInput plugin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = ((User) auth.getPrincipal()).getUsername();
        return pluginMapper.toPlugin(pluginGrpcService.createPlugin(plugin, userId));
    }


    @PreAuthorize("(hasRole(\"AUTHOR\")" +
            " and @pluginGrpcService.isOwnerPlugin(authentication.principal.id,#id))" +
            " or hasRole(\"ADMIN\")")
    @MutationMapping
    public String deletePlugin(@Argument String id) {
        return pluginGrpcService.deletePlugin(id);
    }

    @PreAuthorize("hasAnyRole(\"AUTHOR\",\"ADMIN\")")
    @QueryMapping
    public List<Plugin> getAllPlugins() {
        return pluginMapper.toResponsePlugins(pluginGrpcService.getAllPlugins());
    }

    @PreAuthorize("hasAnyRole(\"AUTHOR\",\"ADMIN\")")
    @QueryMapping
    public Plugin getPlugin(@Argument String id) {
        return pluginMapper.toPlugin(pluginGrpcService.getPlugin(id));
    }

    @PreAuthorize(
            "(hasRole(\"AUTHOR\") and @pluginGrpcService.isOwnerPlugin(authentication.principal.id,#plugin.getId()))" +
                    " or hasRole(\"ADMIN\")"
    )
    @MutationMapping
    public Plugin updatePlugin(@Argument PluginInput plugin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = ((User) auth.getPrincipal()).getUsername();
        return pluginMapper.toPlugin(pluginGrpcService.updatePlugin(plugin, userId));
    }

    @PreAuthorize(
            "(hasRole(\"AUTHOR\") and @pluginGrpcService.isOwnerPlugin(authentication.principal.id,#id))" +
                    " or hasRole(\"ADMIN\")")
    @MutationMapping
    public String validatePlugin(@Argument String id) {
        return pluginGrpcService.validatePlugin(id);
    }
}

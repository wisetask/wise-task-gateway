package ru.leti.wise.task.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.metrics.GraphQlMetrics;
import ru.leti.wise.task.gateway.service.PluginService;
import ru.leti.wise.task.gateway.utils.SecurityUtils;


@Controller
@RequiredArgsConstructor
public class PluginController {

    private final PluginService pluginService;
    private final GraphQlMetrics metrics;

    @PreAuthorize("""
            hasAnyRole("USER", "AUTHOR", "ADMIN")
            """)
    @MutationMapping
    public ImplementationResult checkPluginImplementation(@Argument String id, @Argument String file) {
        metrics.increment("checkPluginImplementation");
        return pluginService.checkPluginImplementation(id, file);
    }

    @PreAuthorize("""
            hasAnyRole("USER", "AUTHOR", "ADMIN")
            """)
    @MutationMapping
    public String checkPluginSolution(@Argument SolutionInput solution) {
        metrics.increment("checkPluginSolution");
        return pluginService.checkPluginSolution(solution);
    }

    @PreAuthorize("""
            hasAnyRole("AUTHOR", "ADMIN")
            """)
    @MutationMapping
    public Plugin createPlugin(@Argument PluginInput plugin) {
        metrics.increment("createPlugin");
        return pluginService.createPlugin(plugin, SecurityUtils.getUserId());
    }


    @PreAuthorize("""
            hasRole("AUTHOR") and @pluginService.isOwnerPlugin(authentication.principal.id, #id) or
            hasRole("ADMIN")
            """)
    @MutationMapping
    public String deletePlugin(@Argument String id) {
        metrics.increment("deletePlugin");
        return pluginService.deletePlugin(id);
    }

    @PreAuthorize("""
            hasAnyRole("AUTHOR", "ADMIN")
            """)
    @QueryMapping
    public GetAllPluginsResponse getAllPlugins(@Argument GetAllPluginRequestInput request) {
        metrics.increment("getAllPlugins");
        return pluginService.getAllPluginsResponse(request);
    }

    @PreAuthorize("""
            hasAnyRole("AUTHOR", "ADMIN")
            """)
    @QueryMapping
    public Plugin getPlugin(@Argument String id) {
        metrics.increment("getPlugin");
        return pluginService.getPlugin(id);
    }

    @PreAuthorize("""
            (hasRole("AUTHOR") and @pluginService.isOwnerPlugin(authentication.principal.id, #plugin.getId())) or
            hasRole("ADMIN")
            """)
    @MutationMapping
    public Plugin updatePlugin(@Argument PluginInput plugin) {
        metrics.increment("updatePlugin");
        return pluginService.updatePlugin(plugin, SecurityUtils.getUserId());
    }

    @PreAuthorize("""
            hasRole("ADMIN")
            """)
    @MutationMapping
    public String validatePlugin(@Argument String id) {
        metrics.increment("validatePlugin");
        return pluginService.validatePlugin(id);
    }
}

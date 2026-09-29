package ru.leti.wise.task.gateway.service.grpc.plugin;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.PluginGrpc;
import ru.leti.wise.task.plugin.PluginGrpc.GetAllPluginRequest;
import ru.leti.wise.task.plugin.PluginGrpc.GetAllPluginsResponse;
import ru.leti.wise.task.plugin.PluginOuterClass;
import ru.leti.wise.task.plugin.PluginServiceGrpc.PluginServiceBlockingStub;

import java.util.List;


@Slf4j
@Component
@Observed
@RequiredArgsConstructor
public class PluginGrpcService {

    private final PluginServiceBlockingStub pluginService;

    public PluginOuterClass.Plugin getPlugin(String id) {
        log.debug("PluginService.getPlugin request, plugin id {}", id);
        var request = PluginGrpc.GetPluginRequest.newBuilder()
                .setId(id)
                .build();

        var plugin = pluginService.getPlugin(request).getPlugin();
        log.debug("PluginService.getPlugin request, fetched plugin {}", plugin.getId());
        return plugin;
    }

    public GetAllPluginsResponse getAllPlugins(GetAllPluginRequest request) {
        log.debug("PluginService.getAllPlugins request, {}", request);
        var response = pluginService.getAllPlugins(request);
        log.debug("PluginService.getAllPlugins request, fetched {} plugins", response.getItemsCount());
        return response;
    }

    public String deletePlugin(String id) {
        log.debug("PluginService.deletePlugin request, plugin id {}", id);
        var request = PluginGrpc.DeletePluginRequest.newBuilder()
                .setId(id)
                .build();

        var deletedId = pluginService.deletePlugin(request).getId();
        log.debug("PluginService.deletePlugin request, deleted plugin {}", deletedId);
        return deletedId;
    }

    public PluginOuterClass.Plugin createPlugin(PluginOuterClass.Plugin plugin) {
        log.debug("PluginService.createPlugin request, plugin id {}", plugin.getId());
        var request = PluginGrpc.CreatePluginRequest.newBuilder()
                .setPlugin(plugin)
                .build();

        var created = pluginService.createPlugin(request).getPlugin();
        log.debug("PluginService.createPlugin request, created plugin {}", created.getId());
        return created;
    }

    public PluginOuterClass.Plugin updatePlugin(PluginOuterClass.Plugin plugin) {
        log.debug("PluginService.updatePlugin request, plugin id {}", plugin.getId());
        var request = PluginGrpc.UpdatePluginRequest.newBuilder()
                .setPlugin(plugin)
                .build();

        var updated = pluginService.updatePlugin(request).getPlugin();
        log.debug("PluginService.updatePlugin request, updated plugin {}", updated.getId());
        return updated;
    }

    public String validatePlugin(String id) {
        log.debug("PluginService.validatePlugin request, plugin id {}", id);
        var request = PluginGrpc.ValidatePluginRequest.newBuilder()
                .setId(id)
                .build();

        var validatedId = pluginService.validatePlugin(request).getId();
        log.debug("PluginService.validatePlugin request, validated plugin {}", validatedId);
        return validatedId;
    }

    public String checkPluginSolution(PluginOuterClass.Solution solution) {
        log.debug("PluginService.checkPluginSolution request, checking plugin solution");
        var request = PluginGrpc.CheckPluginSolutionRequest.newBuilder()
                .setSolution(solution)
                .build();

        var result = pluginService.checkPluginSolution(request).getResult();
        log.debug("PluginService.checkPluginSolution request, checked plugin solution");
        return result;
    }

    public PluginOuterClass.ImplementationResult checkPluginImplementation(String id, String file) {
        log.debug("PluginService.checkPluginImplementation request, plugin id {}", id);
        var request = PluginGrpc.CheckPluginImplementationRequest.newBuilder()
                .setId(id)
                .setFile(file)
                .build();

        var implementationResult = pluginService.checkPluginImplementation(request).getImplementationResult();
        log.debug("PluginService.checkPluginImplementation request, checked plugin implementation {}", id);
        return implementationResult;
    }

    public List<PluginOuterClass.Plugin> getPluginsByIds(List<String> ids) {
        log.debug("PluginService.getPluginsByIds request, plugin ids {}", ids);
        var request = PluginGrpc.PluginIds.newBuilder()
                .addAllPluginIds(ids)
                .build();

        var plugins = pluginService.getPluginsByIds(request).getPluginsList();
        log.debug("PluginService.getPluginsByIds request, fetched {} plugins", plugins.size());
        return plugins;
    }
}

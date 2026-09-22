package ru.leti.wise.task.gateway.service.grpc.plugin;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.plugin.PluginGrpc;
import ru.leti.wise.task.plugin.PluginGrpc.GetAllPluginRequest;
import ru.leti.wise.task.plugin.PluginGrpc.GetAllPluginsResponse;
import ru.leti.wise.task.plugin.PluginOuterClass;
import ru.leti.wise.task.plugin.PluginServiceGrpc.PluginServiceBlockingStub;

import java.util.Collection;
import java.util.List;


@Component
@Observed
@RequiredArgsConstructor
public class PluginGrpcService {

    private final PluginServiceBlockingStub pluginService;

    public PluginOuterClass.Plugin getPlugin(String id) {
        var request = PluginGrpc.GetPluginRequest.newBuilder()
                .setId(id)
                .build();

        return pluginService.getPlugin(request).getPlugin();
    }

    public GetAllPluginsResponse getAllPlugins(GetAllPluginRequest request) {
        return pluginService.getAllPlugins(request);
    }

    public String deletePlugin(String id) {
        var request = PluginGrpc.DeletePluginRequest.newBuilder()
                .setId(id)
                .build();

        return pluginService.deletePlugin(request).getId();
    }

    public PluginOuterClass.Plugin createPlugin(PluginOuterClass.Plugin plugin) {
        var request = PluginGrpc.CreatePluginRequest.newBuilder()
                .setPlugin(plugin)
                .build();

        return pluginService.createPlugin(request).getPlugin();
    }

    public PluginOuterClass.Plugin updatePlugin(PluginOuterClass.Plugin plugin) {
        var request = PluginGrpc.UpdatePluginRequest.newBuilder()
                .setPlugin(plugin)
                .build();

        return pluginService.updatePlugin(request).getPlugin();
    }

    public String validatePlugin(String id) {
        var request = PluginGrpc.ValidatePluginRequest.newBuilder()
                .setId(id)
                .build();

        return pluginService.validatePlugin(request).getId();
    }

    public String checkPluginSolution(PluginOuterClass.Solution solution) {
        var request = PluginGrpc.CheckPluginSolutionRequest.newBuilder()
                .setSolution(solution)
                .build();

        return pluginService.checkPluginSolution(request).getResult();
    }

    public PluginOuterClass.ImplementationResult checkPluginImplementation(String id, String file) {
        var request = PluginGrpc.CheckPluginImplementationRequest.newBuilder()
                .setId(id)
                .setFile(file)
                .build();

        return pluginService.checkPluginImplementation(request).getImplementationResult();
    }

    public List<PluginOuterClass.Plugin> getPluginsByIds(List<String> ids) {
        var request = PluginGrpc.PluginIds.newBuilder()
                .addAllPluginIds(ids)
                .build();

        return pluginService.getPluginsByIds(request).getPluginsList();
    }
}

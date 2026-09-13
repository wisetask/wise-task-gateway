package ru.leti.wise.task.gateway.service.grpc.plugin;

import com.google.protobuf.Empty;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.PluginMapper;
import ru.leti.wise.task.plugin.PluginGrpc;
import ru.leti.wise.task.plugin.PluginOuterClass;
import ru.leti.wise.task.plugin.PluginServiceGrpc.PluginServiceBlockingStub;

import java.util.List;


@Component
@Observed
@RequiredArgsConstructor
public class PluginGrpcService {

    private final PluginServiceBlockingStub pluginService;
    private final PluginMapper pluginMapper;

    public boolean isOwnerPlugin(String userId, String pluginId) {
        var request = PluginGrpc.IsOwnerPluginRequest.newBuilder().setUserId(userId).setPluginId(pluginId).build();
        return pluginService.isOwnerPlugin(request).getResult();
    }

    public PluginOuterClass.ImplementationResult checkPluginImplementation(String id, String file) {
        var request = PluginGrpc.CheckPluginImplementationRequest.newBuilder()
                .setId(id)
                .setFile(file)
                .build();

        return pluginService.checkPluginImplementation(request).getImplementationResult();
    }

    public String checkPluginSolution(SolutionInput solution) {
        var request = PluginGrpc.CheckPluginSolutionRequest.newBuilder()
                .setSolution(pluginMapper.toSolution(solution))
                .build();

        return pluginService.checkPluginSolution(request).getResult();
    }


    public PluginOuterClass.Plugin createPlugin(PluginInput plugin, String authorId) {
        var request = PluginGrpc.CreatePluginRequest.newBuilder()
                .setPlugin(pluginMapper.toPlugin(plugin, authorId))
                .build();

        return pluginService.createPlugin(request).getPlugin();
    }

    public String deletePlugin(String id) {
        var request = PluginGrpc.DeletePluginRequest.newBuilder()
                .setId(id)
                .build();

        return pluginService.deletePlugin(request).getId();
    }

    public List<PluginOuterClass.Plugin> getAllPlugins() {
        return pluginService.getAllPlugins(Empty.newBuilder().build()).getPluginList();
    }

    public PluginOuterClass.Plugin getPlugin(String id) {
        var request = PluginGrpc.GetPluginRequest.newBuilder()
                .setId(id)
                .build();

        return pluginService.getPlugin(request).getPlugin();
    }

    public PluginOuterClass.Plugin updatePlugin(PluginInput plugin, String authorId) {
        var request = PluginGrpc.UpdatePluginRequest.newBuilder()
                .setPlugin(pluginMapper.toPlugin(plugin, authorId))
                .build();

        return pluginService.updatePlugin(request).getPlugin();
    }

    public String validatePlugin(String id) {
        var request = PluginGrpc.ValidatePluginRequest.newBuilder()
                .setId(id)
                .build();

        return pluginService.validatePlugin(request).getId();
    }
}

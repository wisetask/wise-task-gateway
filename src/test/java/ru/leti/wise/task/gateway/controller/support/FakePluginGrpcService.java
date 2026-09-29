package ru.leti.wise.task.gateway.controller.support;

import io.grpc.Status;
import ru.leti.wise.task.gateway.service.grpc.plugin.PluginGrpcService;
import ru.leti.wise.task.plugin.PluginGrpc.GetAllPluginRequest;
import ru.leti.wise.task.plugin.PluginGrpc.GetAllPluginsResponse;
import ru.leti.wise.task.plugin.PluginOuterClass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Фейковый plugin-сервис: отдаёт заранее подготовленные плагины, умеет имитировать gRPC-ошибки.
 */
public class FakePluginGrpcService extends PluginGrpcService {

    private final Map<String, PluginOuterClass.Plugin> plugins = new LinkedHashMap<>();
    private boolean failGetPlugin;
    private boolean failGetPluginsByIds;
    private PluginOuterClass.ImplementationResult implementationResult =
            TestData.implementationResult(TestData.graphTestResult("graph-1"));
    private PluginOuterClass.Solution lastCheckedSolution;

    public FakePluginGrpcService() {
        super(null);
    }

    public void putPlugin(PluginOuterClass.Plugin plugin) {
        plugins.put(plugin.getId(), plugin);
    }

    public void failGetPlugin() {
        failGetPlugin = true;
    }

    public void failGetPluginsByIds() {
        failGetPluginsByIds = true;
    }

    public void setImplementationResult(PluginOuterClass.ImplementationResult implementationResult) {
        this.implementationResult = implementationResult;
    }

    public PluginOuterClass.Solution getLastCheckedSolution() {
        return lastCheckedSolution;
    }

    @Override
    public PluginOuterClass.Plugin getPlugin(String id) {
        if (failGetPlugin) {
            throw Status.UNAVAILABLE.withDescription("plugin service unavailable").asRuntimeException();
        }
        var plugin = plugins.get(id);
        if (plugin == null) {
            throw Status.NOT_FOUND.withDescription("plugin " + id + " not found").asRuntimeException();
        }
        return plugin;
    }

    @Override
    public List<PluginOuterClass.Plugin> getPluginsByIds(List<String> ids) {
        if (failGetPluginsByIds) {
            throw Status.UNAVAILABLE.withDescription("plugin service unavailable").asRuntimeException();
        }
        return ids.stream().map(plugins::get).filter(Objects::nonNull).toList();
    }

    @Override
    public GetAllPluginsResponse getAllPlugins(GetAllPluginRequest request) {
        return GetAllPluginsResponse.newBuilder()
                .addAllItems(plugins.values())
                .setPagination(TestData.pagination())
                .build();
    }

    @Override
    public String deletePlugin(String id) {
        plugins.remove(id);
        return id;
    }

    @Override
    public PluginOuterClass.Plugin createPlugin(PluginOuterClass.Plugin plugin) {
        plugins.put(plugin.getId(), plugin);
        return plugin;
    }

    @Override
    public PluginOuterClass.Plugin updatePlugin(PluginOuterClass.Plugin plugin) {
        plugins.put(plugin.getId(), plugin);
        return plugin;
    }

    @Override
    public String validatePlugin(String id) {
        return id;
    }

    @Override
    public String checkPluginSolution(PluginOuterClass.Solution solution) {
        this.lastCheckedSolution = solution;
        return "OK";
    }

    @Override
    public PluginOuterClass.ImplementationResult checkPluginImplementation(String id, String file) {
        return implementationResult;
    }
}

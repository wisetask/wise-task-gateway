package ru.leti.wise.task.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.PaginationMapper;
import ru.leti.wise.task.gateway.mapper.PluginMapper;
import ru.leti.wise.task.gateway.service.grpc.plugin.PluginGrpcService;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;
import ru.leti.wise.task.plugin.PluginOuterClass;
import ru.leti.wise.task.profile.ProfileOuterClass;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class PluginService {

    private final PluginMapper pluginMapper;
    private final PluginGrpcService pluginGrpcService;
    private final ProfileGrpcService profileGrpcService;
    private final GraphService graphService;
    private final PaginationMapper paginationMapper;
    private final ObjectProvider<PluginService> selfProvider;

    public GetAllPluginsResponse getAllPluginsResponse(GetAllPluginRequestInput request) {
        log.debug("Запрос на получение списка плагинов {}", request);
        var grpcRequest = pluginMapper.toGetAllRequest(request);
        var grpcResponse = pluginGrpcService.getAllPlugins(grpcRequest);
        var items = toPlugins(grpcResponse.getItemsList());
        log.info("Запрос на получение плагинов от пользователя");
        var pagination = paginationMapper.toPagination(grpcResponse.getPagination());
        return GetAllPluginsResponse.newBuilder().items(items).pagination(pagination).build();
    }

    public Plugin getPlugin(String id) {
        return selfProvider.getObject().toPlugin(pluginGrpcService.getPlugin(id));
    }

    public boolean isOwnerPlugin(String userId, String pluginId) {
        return pluginGrpcService.getPlugin(pluginId).getAuthorId().equals(userId);
    }

    @CachePut(value = "plugin", key = "#result.id", cacheManager = "localCacheManager")
    public Plugin createPlugin(PluginInput plugin, String authorId) {
        return selfProvider.getObject().toPlugin(pluginGrpcService.createPlugin(pluginMapper.toPlugin(plugin, authorId)));
    }

    @CachePut(value = "plugin", key = "#plugin.id", cacheManager = "localCacheManager")
    public Plugin updatePlugin(PluginInput plugin, String authorId) {
        return selfProvider.getObject().toPlugin(pluginGrpcService.updatePlugin(pluginMapper.toPlugin(plugin, authorId)));
    }

    @CacheEvict(value = "plugin", key = "#id", cacheManager = "localCacheManager")
    public String deletePlugin(String id) {
        return pluginGrpcService.deletePlugin(id);
    }

    public String validatePlugin(String id) {
        return pluginGrpcService.validatePlugin(id);
    }

    public String checkPluginSolution(SolutionInput solution) {
        return pluginGrpcService.checkPluginSolution(pluginMapper.toSolution(solution));
    }

    public ImplementationResult checkPluginImplementation(String id, String file) {
        var grpcResult = pluginGrpcService.checkPluginImplementation(id, file);
        var result = pluginMapper.toImplementationResult(grpcResult);
        result.setGraphTestResults(toGraphTestResults(grpcResult.getGraphTestResultsList()));

        return result;
    }

    private List<GraphTestResult> toGraphTestResults(List<PluginOuterClass.GraphTestResult> graphTestResults) {
        var graphs = graphService.getGraphsByIds(graphTestResults.stream().map(PluginOuterClass.GraphTestResult::getGraphId).toList());

        return graphTestResults.stream().map(graphTestResult -> {
            var mapped = pluginMapper.toGraphTestResult(graphTestResult);
            mapped.setGraph(graphs.get(graphTestResult.getGraphId()));

            return mapped;
        }).toList();
    }

    @Cacheable(value = "plugin", key = "#grpcPlugin.id", cacheManager = "localCacheManager")
    public Plugin toPlugin(PluginOuterClass.Plugin grpcPlugin) {
        return pluginMapper.toPlugin(grpcPlugin, getGrpcProfile(grpcPlugin.getAuthorId()));
    }

    private List<Plugin> toPlugins(List<PluginOuterClass.Plugin> grpcPlugins) {
        var profiles = getProfilesByIds(grpcPlugins.stream().map(PluginOuterClass.Plugin::getAuthorId).toList());

        return grpcPlugins.stream()
                .map(grpcPlugin -> pluginMapper.toPlugin(grpcPlugin, profiles.get(grpcPlugin.getAuthorId())))
                .toList();
    }

    private ProfileOuterClass.Profile getGrpcProfile(String profileId) {
        return profileGrpcService.getProfile(profileId);
    }

    private Map<String, ProfileOuterClass.Profile> getProfilesByIds(List<String> profileIds) {
        var ids = profileIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        return profileGrpcService.getProfilesByIds(ids).stream()
                .collect(Collectors.toMap(
                        ProfileOuterClass.Profile::getId,
                        profile -> profile
                ));
    }
}

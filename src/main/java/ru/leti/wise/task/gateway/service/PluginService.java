package ru.leti.wise.task.gateway.service;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
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
    @Lazy @Autowired private PluginService self;

    public GetAllPluginsResponse getAllPluginsResponse(GetAllPluginRequestInput request) {
        log.debug("getAllPlugins request, {}", request);
        var grpcRequest = pluginMapper.toGetAllRequest(request);
        var grpcResponse = pluginGrpcService.getAllPlugins(grpcRequest);
        log.info("getAllPlugins request, fetched {} plugins", grpcResponse.getItemsCount());
        var items = toPlugins(grpcResponse.getItemsList());
        var pagination = paginationMapper.toPagination(grpcResponse.getPagination());
        return GetAllPluginsResponse.newBuilder().items(items).pagination(pagination).build();
    }

    public Plugin getPlugin(String id) {
        log.debug("getPlugin request, {}", id);
        var plugin = self.toPlugin(pluginGrpcService.getPlugin(id));
        log.info("getPlugin request, mapped plugin {}", id);
        return plugin;
    }

    public boolean isOwnerPlugin(String userId, String pluginId) {
        log.debug("isOwnerPlugin request, plugin {} owner {}", pluginId, userId);
        return pluginGrpcService.getPlugin(pluginId).getAuthorId().equals(userId);
    }

    @CachePut(value = "plugin", key = "#result.id", cacheManager = "localCacheManager")
    public Plugin createPlugin(PluginInput plugin, String authorId) {
        log.debug("createPlugin request, {}", plugin);
        var created = self.toPlugin(pluginGrpcService.createPlugin(pluginMapper.toPlugin(plugin, authorId)));
        log.info("createPlugin request, created plugin {}", created.getId());
        return created;
    }

    @CachePut(value = "plugin", key = "#plugin.id", cacheManager = "localCacheManager")
    public Plugin updatePlugin(PluginInput plugin, String authorId) {
        log.debug("updatePlugin request, {}", plugin);
        var updated = self.toPlugin(pluginGrpcService.updatePlugin(pluginMapper.toPlugin(plugin, authorId)));
        log.info("updatePlugin request, updated plugin {}", updated.getId());
        return updated;
    }

    @CacheEvict(value = "plugin", key = "#id", cacheManager = "localCacheManager")
    public String deletePlugin(String id) {
        log.debug("deletePlugin request, {}", id);
        pluginGrpcService.deletePlugin(id);
        log.info("deletePlugin request, deleted plugin {}", id);
        return id;
    }

    public String validatePlugin(String id) {
        log.debug("validatePlugin request, {}", id);
        var validatedId = pluginGrpcService.validatePlugin(id);
        log.info("validatePlugin request, validated plugin {}", validatedId);
        return validatedId;
    }

    public String checkPluginSolution(SolutionInput solution) {
        log.debug("checkPluginSolution request, {}", solution);
        var result = pluginGrpcService.checkPluginSolution(pluginMapper.toSolution(solution));
        log.info("checkPluginSolution request, checked plugin solution");
        return result;
    }

    public ImplementationResult checkPluginImplementation(String id, String file) {
        log.debug("checkPluginImplementation request, plugin {}", id);
        var grpcResult = pluginGrpcService.checkPluginImplementation(id, file);
        var result = pluginMapper.toImplementationResult(grpcResult);
        result.setGraphTestResults(toGraphTestResults(grpcResult.getGraphTestResultsList()));
        log.info("checkPluginImplementation request, checked plugin implementation {}", id);

        return result;
    }

    private List<GraphTestResult> toGraphTestResults(List<PluginOuterClass.GraphTestResult> graphTestResults) {
        var graphIds = graphTestResults.stream().map(PluginOuterClass.GraphTestResult::getGraphId).toList();
        log.debug("checkPluginImplementation mapping, mapping {} graph test results", graphTestResults.size());
        var graphs = getGraphsForTestResults(graphIds);

        return graphTestResults.stream().map(graphTestResult -> {
            var mapped = pluginMapper.toGraphTestResult(graphTestResult);
            mapped.setGraph(graphs.get(graphTestResult.getGraphId()));

            return mapped;
        }).toList();
    }

    private Map<String, Graph> getGraphsForTestResults(List<String> graphIds) {
        try {
            return graphService.getGraphsByIds(graphIds);
        } catch (StatusRuntimeException e) {
            log.warn("checkPluginImplementation mapping, failed to fetch graphs {}, returning empty map", graphIds, e);
            return Map.of();
        }
    }

    @Cacheable(value = "plugin", key = "#grpcPlugin.id", cacheManager = "localCacheManager")
    public Plugin toPlugin(PluginOuterClass.Plugin grpcPlugin) {
        log.debug("toPlugin mapping, mapping plugin {} with author", grpcPlugin.getId());
        return pluginMapper.toPlugin(grpcPlugin, getGrpcProfile(grpcPlugin.getAuthorId()));
    }

    private List<Plugin> toPlugins(List<PluginOuterClass.Plugin> grpcPlugins) {
        var profiles = getProfilesByIds(grpcPlugins.stream().map(PluginOuterClass.Plugin::getAuthorId).toList());
        log.debug("getAllPlugins request, mapping {} plugins with authors", grpcPlugins.size());

        return grpcPlugins.stream()
                .map(grpcPlugin -> pluginMapper.toPlugin(grpcPlugin, profiles.get(grpcPlugin.getAuthorId())))
                .toList();
    }

    private ProfileOuterClass.Profile getGrpcProfile(String profileId) {
        log.debug("mapping, fetching profile {}", profileId);
        try {
            return profileGrpcService.getProfile(profileId);
        } catch (StatusRuntimeException e) {
            log.warn("mapping, failed to fetch profile {}, returning null profile", profileId, e);
            return null;
        }
    }

    private Map<String, ProfileOuterClass.Profile> getProfilesByIds(List<String> profileIds) {
        var ids = profileIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        log.debug("mapping, fetching profiles {}", ids);
        try {
            return profileGrpcService.getProfilesByIds(ids).stream()
                    .collect(Collectors.toMap(
                            ProfileOuterClass.Profile::getId,
                            profile -> profile
                    ));
        } catch (StatusRuntimeException e) {
            log.warn("mapping, failed to fetch profiles {}, returning empty map", ids, e);
            return Map.of();
        }
    }
}

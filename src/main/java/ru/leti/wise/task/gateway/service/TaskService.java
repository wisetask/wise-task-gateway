package ru.leti.wise.task.gateway.service;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.*;
import ru.leti.wise.task.gateway.service.grpc.plugin.PluginGrpcService;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;
import ru.leti.wise.task.gateway.service.grpc.task.TaskGrpcService;
import ru.leti.wise.task.plugin.PluginOuterClass;
import ru.leti.wise.task.profile.ProfileOuterClass;
import ru.leti.wise.task.task.TaskOuterClass;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class TaskService {
    @Autowired private TaskMapper taskMapper;
    @Autowired private SolutionMapper solutionMapper;
    @Autowired private ProfileMapper profileMapper;
    @Autowired private PluginMapper pluginMapper;
    @Autowired private TaskGrpcService taskGrpcService;
    @Autowired private ProfileGrpcService profileGrpcService;
    @Autowired private PluginGrpcService pluginGrpcService;
    @Autowired private PluginService pluginService;
    @Autowired private PaginationMapper paginationMapper;
    @Lazy @Autowired private TaskService self;

    public GetAllTaskResponse getAllTaskResponse(GetAllTaskRequestInput request) {
        log.debug("getAllTasks request, {}", request);
        var grpcRequest = taskMapper.toGetAllRequest(request);
        var grpcResponse = taskGrpcService.getAllTasks(grpcRequest);
        log.info("getAllTasks request, fetched {} tasks", grpcResponse.getItemsCount());
        var items = toTasks(grpcResponse.getItemsList());
        var pagination = paginationMapper.toPagination(grpcResponse.getPagination());
        return GetAllTaskResponse.newBuilder().items(items).pagination(pagination).build();
    }

    public GetAllTaskSolutionsResponse getAllTaskSolutionsResponse(GetAllTaskSolutionsRequestInput request) {
        log.debug("getAllTaskSolutions request, {}", request);
        var grpcRequest = solutionMapper.toGetAllRequest(request);
        var grpcResponse = taskGrpcService.getAllTaskSolutions(grpcRequest);
        log.info("getAllTaskSolutions request, fetched {} task solutions", grpcResponse.getItemsCount());
        var items = toSolutions(grpcResponse.getItemsList());
        var pagination = paginationMapper.toPagination(grpcResponse.getPagination());
        return GetAllTaskSolutionsResponse.newBuilder().items(items).pagination(pagination).build();
    }

    public Task getTask(String id) {
        log.debug("getTask request, {}", id);
        var task = self.toTask(taskGrpcService.getTask(id));
        log.info("getTask request, mapped task {}", id);
        return task;
    }

    public Solution getTaskSolution(String id) {
        log.debug("getTaskSolution request, {}", id);
        var solution = self.toSolution(taskGrpcService.getTaskSolution(id));
        log.info("getTaskSolution request, mapped task solution {}", id);
        return solution;
    }

    @CachePut(value = "task", key = "#result.id", cacheManager = "localCacheManager")
    public TaskGraph createTaskGraph(TaskGraphInput task, String authorId) {
        log.debug("createTaskGraph request, {}", task);
        var grpcTask = taskGrpcService.createTask(taskMapper.toTaskGraph(task, authorId));
        log.info("createTaskGraph request, created task graph {}", grpcTask.getId());
        return (TaskGraph) toTask(grpcTask);
    }

    @CachePut(value = "task", key = "#result.id", cacheManager = "localCacheManager")
    public TaskImplementation createTaskImplementation(TaskImplementationInput task, String authorId) {
        log.debug("createTaskImplementation request, {}", task);
        var grpcTask = taskGrpcService.createTask(taskMapper.toTaskImplementation(task, authorId));
        log.info("createTaskImplementation request, created task implementation {}", grpcTask.getId());
        return (TaskImplementation) toTask(grpcTask);
    }

    @CachePut(value = "task", key = "#task.id", cacheManager = "localCacheManager")
    public TaskGraph updateTaskGraph(TaskGraphInput task, String authorId) {
        log.debug("updateTaskGraph request, {}", task);
        var grpcTask = taskGrpcService.updateTask(taskMapper.toTaskGraph(task, authorId));
        log.info("updateTaskGraph request, updated task graph {}", grpcTask.getId());
        return (TaskGraph) toTask(grpcTask);
    }

    @CachePut(value = "task", key = "#task.id", cacheManager = "localCacheManager")
    public TaskImplementation updateTaskImplementation(TaskImplementationInput task, String authorId) {
        log.debug("updateTaskImplementation request, {}", task);
        var grpcTask = taskGrpcService.updateTask(taskMapper.toTaskImplementation(task, authorId));
        log.info("updateTaskImplementation request, updated task implementation {}", grpcTask.getId());
        return (TaskImplementation) toTask(grpcTask);
    }

    @CacheEvict(value = "task", key = "#id", cacheManager = "localCacheManager")
    public String deleteTask(String id) {
        log.debug("deleteTask request, {}", id);
        taskGrpcService.deleteTask(id);
        log.info("deleteTask request, deleted task {}", id);
        return id;
    }

    @CachePut(value = "solution", key = "#result.id", cacheManager = "localCacheManager")
    public SolutionGraph solveTaskGraph(SolutionGraphInput solution, String authorId) {
        log.debug("solveTaskGraph request, {}", solution);
        var grpcSolution = taskGrpcService.solveTask(solutionMapper.toSolutionGraph(solution, authorId));
        var solved = (SolutionGraph) self.toSolution(grpcSolution);
        log.info("solveTaskGraph request, solved task graph {}", solved.getId());
        return solved;
    }

    @CachePut(value = "solution", key = "#result.id", cacheManager = "localCacheManager")
    public SolutionImplementation solveTaskImplementation(SolutionImplementationInput solution, String authorId) {
        log.debug("solveTaskImplementation request, {}", solution);
        var grpcSolution = taskGrpcService.solveTask(solutionMapper.toSolutionImplementation(solution, authorId));
        var solved = (SolutionImplementation) self.toSolution(grpcSolution);
        log.info("solveTaskImplementation request, solved task implementation {}", solved.getId());
        return solved;
    }

    @Cacheable(value = "task", key = "#grpcTask.id", cacheManager = "localCacheManager")
    public Task toTask(TaskOuterClass.Task grpcTask) {
        log.debug("toTask mapping, mapping task {} with author", grpcTask.getId());
        return toTask(grpcTask, getGrpcProfile(grpcTask.getAuthorId()));
    }

    private Task toTask(TaskOuterClass.Task grpcTask, ProfileOuterClass.Profile author) {
        if (grpcTask == null) {
            return null;
        }

        var task = taskMapper.toTask(grpcTask, author);
        enrichTask(task, grpcTask);

        return task;
    }

    private Task toTask(TaskOuterClass.Task grpcTask, Map<String, ProfileOuterClass.Profile> profiles) {
        if (grpcTask == null) {
            return null;
        }

        return toTask(grpcTask, profiles.get(grpcTask.getAuthorId()));
    }

    @Cacheable(value = "solution", key = "#grpcSolution.id", cacheManager = "localCacheManager")
    public Solution toSolution(TaskOuterClass.Solution grpcSolution) {
        log.debug("toSolution mapping, mapping solution {}", grpcSolution.getId());

        Task task;
        try {
            task = self.toTask(taskGrpcService.getTask(grpcSolution.getTaskId()));
        } catch (StatusRuntimeException e) {
            log.warn("toSolution mapping, failed to fetch task {}, returning solution without task",
                    grpcSolution.getTaskId(), e);
            task = null;
        }

        return toSolution(grpcSolution, task, getProfile(grpcSolution.getAuthorId()));
    }

    private Solution toSolution(TaskOuterClass.Solution grpcSolution, Task task, Profile author) {
        var solution = solutionMapper.toSolution(grpcSolution, task, author);
        enrichSolution(solution, grpcSolution);

        return solution;
    }

    private List<Task> toTasks(List<TaskOuterClass.Task> grpcTasks) {
        var profiles = getProfilesByIds(grpcTasks.stream().map(TaskOuterClass.Task::getAuthorId).toList());
        log.debug("getAllTasks request, mapping {} tasks with authors", grpcTasks.size());

        return grpcTasks.stream()
                .map(grpcTask -> toTask(grpcTask, profiles))
                .toList();
    }

    private List<Solution> toSolutions(List<TaskOuterClass.Solution> grpcSolutions) {
        var profiles = getProfilesByIds(grpcSolutions.stream().map(TaskOuterClass.Solution::getAuthorId).toList());
        var tasks = getTasksByIds(grpcSolutions.stream().map(TaskOuterClass.Solution::getTaskId).toList());
        var taskProfiles = getProfilesByIds(tasks.values().stream().map(TaskOuterClass.Task::getAuthorId).toList());
        log.debug("getAllTaskSolutions request, mapping {} solutions with tasks and authors", grpcSolutions.size());

        return grpcSolutions.stream()
                .map(grpcSolution -> toSolution(
                        grpcSolution,
                        toTask(tasks.get(grpcSolution.getTaskId()), taskProfiles),
                        profileMapper.toProfile(profiles.get(grpcSolution.getAuthorId()))
                ))
                .toList();
    }

    private void enrichTask(Task task, TaskOuterClass.Task grpcTask) {
        if (task instanceof TaskGraph taskGraph) {
            log.debug("toTask mapping, enriching task graph {} with condition plugins", task.getId());
            taskGraph.setCondition(toPluginInfos(grpcTask.getTaskGraph().getConditionList()));
        } else if (task instanceof TaskImplementation taskImplementation) {
            log.debug("toTask mapping, enriching task implementation {} with plugin", task.getId());
            taskImplementation.setPlugin(getPlugin(grpcTask.getTaskImplementation().getPluginId()));
        }
    }

    private void enrichSolution(Solution solution, TaskOuterClass.Solution grpcSolution) {
        if (solution instanceof SolutionGraph solutionGraph) {
            log.debug("toSolution mapping, enriching solution graph {} with plugin results", solution.getId());
            solutionGraph.setPluginResults(toPluginResults(grpcSolution.getSolutionGraph().getPluginResultsList()));
        }
    }

    private List<PluginInfo> toPluginInfos(List<TaskOuterClass.PluginInfo> pluginInfos) {
        var plugins = getPluginsByIds(pluginInfos.stream().map(TaskOuterClass.PluginInfo::getPluginId).toList());
        log.debug("task mapping, mapping {} task condition plugins", pluginInfos.size());

        return pluginInfos.stream().map(pluginInfo -> {
            var mapped = taskMapper.toPluginInfo(pluginInfo);
            var grpcPlugin = plugins.get(pluginInfo.getPluginId());
            if (grpcPlugin == null) {
                log.warn("task mapping, plugin {} not found, returning null plugin", pluginInfo.getPluginId());
                mapped.setPlugin(null);
            } else {
                mapped.setPlugin(pluginService.toPlugin(grpcPlugin));
            }

            return mapped;
        }).toList();
    }

    private List<PluginResult> toPluginResults(List<TaskOuterClass.PluginResult> pluginResults) {
        var plugins = getPluginsByIds(pluginResults.stream().map(TaskOuterClass.PluginResult::getPluginId).toList());
        log.debug("solution mapping, mapping {} solution plugin results", pluginResults.size());

        return pluginResults.stream().map(pluginResult -> {
            var mapped = solutionMapper.toPluginResult(pluginResult);
            var grpcPlugin = plugins.get(pluginResult.getPluginId());
            if (grpcPlugin == null) {
                log.warn("solution mapping, plugin {} not found, returning null plugin", pluginResult.getPluginId());
                mapped.setPlugin(null);
            } else {
                mapped.setPlugin(pluginService.toPlugin(grpcPlugin));
            }

            return mapped;
        }).toList();
    }

    private Plugin getPlugin(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            log.debug("task mapping, task implementation has no plugin id, skipping plugin enrichment");
            return null;
        }

        log.debug("task mapping, fetching plugin {}", pluginId);
        PluginOuterClass.Plugin grpcPlugin;
        try {
            grpcPlugin = pluginGrpcService.getPlugin(pluginId);
        } catch (StatusRuntimeException e) {
            log.warn("task mapping, failed to fetch plugin {}, returning null plugin", pluginId, e);
            return null;
        }

        return pluginMapper.toPlugin(grpcPlugin, getGrpcProfile(grpcPlugin.getAuthorId()));
    }

    private Map<String, PluginOuterClass.Plugin> getPluginsByIds(List<String> pluginIds) {
        var ids = pluginIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        log.debug("task mapping, fetching plugins {}", ids);
        try {
            return pluginGrpcService.getPluginsByIds(ids).stream()
                    .collect(Collectors.toMap(
                            PluginOuterClass.Plugin::getId,
                            plugin -> plugin
                    ));
        } catch (StatusRuntimeException e) {
            log.warn("task mapping, failed to fetch plugins {}, returning empty map", ids, e);
            return Map.of();
        }
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

    private Profile getProfile(String profileId) {
        return profileMapper.toProfile(getGrpcProfile(profileId));
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

    private Map<String, TaskOuterClass.Task> getTasksByIds(List<String> taskIds) {
        var ids = taskIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        log.debug("mapping, fetching tasks {}", ids);
        try {
            return taskGrpcService.getTasksByIds(ids).stream()
                    .collect(Collectors.toMap(
                            TaskOuterClass.Task::getId,
                            task -> task
                    ));
        } catch (StatusRuntimeException e) {
            log.warn("mapping, failed to fetch tasks {}, returning empty map", ids, e);
            return Map.of();
        }
    }
}

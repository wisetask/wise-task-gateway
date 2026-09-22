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
import ru.leti.wise.task.gateway.mapper.ProfileMapper;
import ru.leti.wise.task.gateway.mapper.SolutionMapper;
import ru.leti.wise.task.gateway.mapper.TaskMapper;
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
@Component
@RequiredArgsConstructor
public class TaskService {
    private final TaskMapper taskMapper;
    private final SolutionMapper solutionMapper;
    private final ProfileMapper profileMapper;
    private final PluginMapper pluginMapper;
    private final TaskGrpcService taskGrpcService;
    private final ProfileGrpcService profileGrpcService;
    private final PluginGrpcService pluginGrpcService;
    private final PluginService pluginService;
    private final PaginationMapper paginationMapper;
    private final ObjectProvider<TaskService> selfProvider;

    public GetAllTaskResponse getAllTaskResponse(GetAllTaskRequestInput request) {
        log.debug("Запрос на получение списка задач {}", request);
        var grpcRequest = taskMapper.toGetAllRequest(request);
        var grpcResponse = taskGrpcService.getAllTasks(grpcRequest);
        var items = toTasks(grpcResponse.getItemsList());
        log.info("Запрос на получение задач от пользователя");
        var pagination = paginationMapper.toPagination(grpcResponse.getPagination());
        return GetAllTaskResponse.newBuilder().items(items).pagination(pagination).build();
    }

    public GetAllTaskSolutionsResponse getAllTaskSolutionsResponse(GetAllTaskSolutionsRequestInput request) {
        log.debug("Запрос на получение списка решений задач {}", request);
        var grpcRequest = solutionMapper.toGetAllRequest(request);
        var grpcResponse = taskGrpcService.getAllTaskSolutions(grpcRequest);
        var items = toSolutions(grpcResponse.getItemsList());
        log.info("Запрос на получение решений задач от пользователя");
        var pagination = paginationMapper.toPagination(grpcResponse.getPagination());
        return GetAllTaskSolutionsResponse.newBuilder().items(items).pagination(pagination).build();
    }

    public Task getTask(String id) {
        return selfProvider.getObject().toTask(taskGrpcService.getTask(id));
    }

    public Solution getTaskSolution(String id) {
        return selfProvider.getObject().toSolution(taskGrpcService.getTaskSolution(id));
    }

    @CachePut(value = "task", key = "#result.id", cacheManager = "localCacheManager")
    public TaskGraph createTaskGraph(TaskGraphInput task, String authorId) {
        var grpcTask = taskGrpcService.createTask(taskMapper.toTaskGraph(task, authorId));
        return (TaskGraph) toTask(grpcTask);
    }

    @CachePut(value = "task", key = "#result.id", cacheManager = "localCacheManager")
    public TaskImplementation createTaskImplementation(TaskImplementationInput task, String authorId) {
        var grpcTask = taskGrpcService.createTask(taskMapper.toTaskImplementation(task, authorId));
        return (TaskImplementation) toTask(grpcTask);
    }

    @CachePut(value = "task", key = "#task.id", cacheManager = "localCacheManager")
    public TaskGraph updateTaskGraph(TaskGraphInput task, String authorId) {
        var grpcTask = taskGrpcService.updateTask(taskMapper.toTaskGraph(task, authorId));
        return (TaskGraph) toTask(grpcTask);
    }

    @CachePut(value = "task", key = "#task.id", cacheManager = "localCacheManager")
    public TaskImplementation updateTaskImplementation(TaskImplementationInput task, String authorId) {
        var grpcTask = taskGrpcService.updateTask(taskMapper.toTaskImplementation(task, authorId));
        return (TaskImplementation) toTask(grpcTask);
    }

    @CacheEvict(value = "task", key = "#id", cacheManager = "localCacheManager")
    public String deleteTask(String id) {
        taskGrpcService.deleteTask(id);
        return id;
    }

    @CachePut(value = "solution", key = "#result.id", cacheManager = "localCacheManager")
    public SolutionGraph solveTaskGraph(SolutionGraphInput solution, String authorId) {
        var grpcSolution = taskGrpcService.solveTask(solutionMapper.toSolutionGraph(solution, authorId));
        return (SolutionGraph) selfProvider.getObject().toSolution(grpcSolution);
    }

    @CachePut(value = "solution", key = "#result.id", cacheManager = "localCacheManager")
    public SolutionImplementation solveTaskImplementation(SolutionImplementationInput solution, String authorId) {
        var grpcSolution = taskGrpcService.solveTask(solutionMapper.toSolutionImplementation(solution, authorId));
        return (SolutionImplementation) selfProvider.getObject().toSolution(grpcSolution);
    }

    @Cacheable(value = "task", key = "#grpcTask.id", cacheManager = "localCacheManager")
    public Task toTask(TaskOuterClass.Task grpcTask) {
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
        return toSolution(
                grpcSolution,
                getTask(grpcSolution.getTaskId()),
                getProfile(grpcSolution.getAuthorId())
        );
    }

    private Solution toSolution(TaskOuterClass.Solution grpcSolution, Task task, Profile author) {
        var solution = solutionMapper.toSolution(grpcSolution, task, author);
        enrichSolution(solution, grpcSolution);

        return solution;
    }

    private List<Task> toTasks(List<TaskOuterClass.Task> grpcTasks) {
        var profiles = getProfilesByIds(grpcTasks.stream().map(TaskOuterClass.Task::getAuthorId).toList());

        return grpcTasks.stream()
                .map(grpcTask -> selfProvider.getObject().toTask(grpcTask, profiles))
                .toList();
    }

    private List<Solution> toSolutions(List<TaskOuterClass.Solution> grpcSolutions) {
        var profiles = getProfilesByIds(grpcSolutions.stream().map(TaskOuterClass.Solution::getAuthorId).toList());
        var tasks = getTasksByIds(grpcSolutions.stream().map(TaskOuterClass.Solution::getTaskId).toList());
        var taskProfiles = getProfilesByIds(tasks.values().stream().map(TaskOuterClass.Task::getAuthorId).toList());

        return grpcSolutions.stream()
                .map(grpcSolution -> toSolution(
                        grpcSolution,
                        selfProvider.getObject().toTask(tasks.get(grpcSolution.getTaskId()), taskProfiles),
                        profileMapper.toProfile(profiles.get(grpcSolution.getAuthorId()))
                ))
                .toList();
    }

    private void enrichTask(Task task, TaskOuterClass.Task grpcTask) {
        if (task instanceof TaskGraph taskGraph) {
            taskGraph.setCondition(toPluginInfos(grpcTask.getTaskGraph().getConditionList()));
        } else if (task instanceof TaskImplementation taskImplementation) {
            taskImplementation.setPlugin(getPlugin(grpcTask.getTaskImplementation().getPluginId()));
        }
    }

    private void enrichSolution(Solution solution, TaskOuterClass.Solution grpcSolution) {
        if (solution instanceof SolutionGraph solutionGraph) {
            solutionGraph.setPluginResults(toPluginResults(grpcSolution.getSolutionGraph().getPluginResultsList()));
        }
    }

    private List<PluginInfo> toPluginInfos(List<TaskOuterClass.PluginInfo> pluginInfos) {
        var plugins = getPluginsByIds(pluginInfos.stream().map(TaskOuterClass.PluginInfo::getPluginId).toList());

        return pluginInfos.stream().map(pluginInfo -> {
            var mapped = taskMapper.toPluginInfo(pluginInfo);
            mapped.setPlugin(pluginService.toPlugin(plugins.get(pluginInfo.getPluginId())));

            return mapped;
        }).toList();
    }

    private List<PluginResult> toPluginResults(List<TaskOuterClass.PluginResult> pluginResults) {
        var plugins = getPluginsByIds(pluginResults.stream().map(TaskOuterClass.PluginResult::getPluginId).toList());

        return pluginResults.stream().map(pluginResult -> {
            var mapped = solutionMapper.toPluginResult(pluginResult);
            mapped.setPlugin(pluginService.toPlugin(plugins.get(pluginResult.getPluginId())));

            return mapped;
        }).toList();
    }

    private Plugin getPlugin(String pluginId) {
        var grpcPlugin = pluginGrpcService.getPlugin(pluginId);
        return pluginMapper.toPlugin(grpcPlugin, getGrpcProfile(grpcPlugin.getAuthorId()));
    }

    private Map<String, PluginOuterClass.Plugin> getPluginsByIds(List<String> pluginIds) {
        var ids = pluginIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        return pluginGrpcService.getPluginsByIds(ids).stream()
                .collect(Collectors.toMap(
                        PluginOuterClass.Plugin::getId,
                        plugin -> plugin
                ));
    }

    private ProfileOuterClass.Profile getGrpcProfile(String profileId) {
        return profileGrpcService.getProfile(profileId);
    }

    private Profile getProfile(String profileId) {
        return profileMapper.toProfile(getGrpcProfile(profileId));
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

    private Map<String, TaskOuterClass.Task> getTasksByIds(List<String> taskIds) {
        var ids = taskIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        return taskGrpcService.getTasksByIds(ids).stream()
                .collect(Collectors.toMap(
                        TaskOuterClass.Task::getId,
                        task -> task
                ));
    }
}

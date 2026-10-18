package ru.leti.wise.task.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.PaginationMapper;
import ru.leti.wise.task.gateway.mapper.TaskMapper;
import ru.leti.wise.task.gateway.service.grpc.TaskGrpcService;
import ru.leti.wise.task.task.TaskOuterClass;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskMapper taskMapper;
    private final TaskGrpcService taskGrpcService;
    private final PaginationMapper paginationMapper;

    public GetAllTaskResponse getAllTaskResponse(GetAllTaskRequestInput request) {
        log.debug("getAllTasks request, {}", request);
        var grpcRequest = taskMapper.toGetAllRequest(request);
        var grpcResponse = taskGrpcService.getAllTasks(grpcRequest);
        log.info("getAllTasks request, fetched {} tasks", grpcResponse.getItemsCount());
        var items = toTasks(grpcResponse.getItemsList());
        var pagination = paginationMapper.toPagination(grpcResponse.getPagination());
        return GetAllTaskResponse.newBuilder().items(items).pagination(pagination).build();
    }

    @Cacheable(value = "task", key = "#id", cacheManager = "localCacheManager")
    public Task getTask(String id) {
        log.debug("getTask request, {}", id);
        var taskGrpc = taskGrpcService.getTask(id);
        var task = toTask(taskGrpc);
        log.info("getTask request, mapped task {}", id);
        return task;
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

    public Task toTask(TaskOuterClass.Task grpcTask) {
        log.debug("toTask mapping, mapping task {}", grpcTask.getId());
        return taskMapper.toTask(grpcTask);
    }

    private List<Task> toTasks(List<TaskOuterClass.Task> grpcTasks) {
        log.debug("getAllTasks request, mapping {} tasks with authors", grpcTasks.size());
        return grpcTasks.stream()
                .map(this::toTask)
                .toList();
    }
}

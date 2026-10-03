package ru.leti.wise.task.gateway.controller.support;

import io.grpc.Status;
import ru.leti.wise.task.gateway.service.grpc.task.TaskGrpcService;
import ru.leti.wise.task.task.TaskGrpc.GetAllTaskRequest;
import ru.leti.wise.task.task.TaskGrpc.GetAllTaskResponse;
import ru.leti.wise.task.task.TaskGrpc.GetAllTaskSolutionsRequest;
import ru.leti.wise.task.task.TaskGrpc.GetAllTaskSolutionsResponse;
import ru.leti.wise.task.task.TaskOuterClass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Фейковый task-сервис: отдаёт заранее подготовленные данные, умеет имитировать
 * gRPC-ошибки (чтобы проверить enrichment).
 */
public class FakeTaskGrpcService extends TaskGrpcService {

    private final Map<String, TaskOuterClass.Task> tasks = new LinkedHashMap<>();
    private final Map<String, TaskOuterClass.Solution> solutions = new LinkedHashMap<>();
    private boolean failGetTasksByIds;

    public FakeTaskGrpcService() {
        super(null);
    }

    public void putTask(TaskOuterClass.Task task) {
        tasks.put(task.getId(), task);
    }

    public void putSolution(TaskOuterClass.Solution solution) {
        solutions.put(solution.getId(), solution);
    }

    public void failGetTasksByIds() {
        failGetTasksByIds = true;
    }

    @Override
    public TaskOuterClass.Task getTask(String id) {
        var task = tasks.get(id);
        if (task == null) {
            throw Status.NOT_FOUND.withDescription("task " + id + " not found").asRuntimeException();
        }
        return task;
    }

    @Override
    public List<TaskOuterClass.Task> getTasksByIds(List<String> taskIds) {
        if (failGetTasksByIds) {
            throw Status.UNAVAILABLE.withDescription("task service unavailable").asRuntimeException();
        }
        return taskIds.stream().map(tasks::get).filter(Objects::nonNull).toList();
    }

    @Override
    public GetAllTaskResponse getAllTasks(GetAllTaskRequest request) {
        return GetAllTaskResponse.newBuilder()
                .addAllItems(tasks.values())
                .setPagination(TestData.pagination())
                .build();
    }

    @Override
    public void deleteTask(String id) {
        tasks.remove(id);
    }

    @Override
    public TaskOuterClass.Task createTask(TaskOuterClass.Task task) {
        tasks.put(task.getId(), task);
        return task;
    }

    @Override
    public TaskOuterClass.Task updateTask(TaskOuterClass.Task task) {
        tasks.put(task.getId(), task);
        return task;
    }

    @Override
    public TaskOuterClass.Solution solveTask(TaskOuterClass.Solution solutionRequest) {
        solutions.put(solutionRequest.getId(), solutionRequest);
        return solutionRequest;
    }

    @Override
    public TaskOuterClass.Solution getTaskSolution(String id) {
        var solution = solutions.get(id);
        if (solution == null) {
            throw Status.NOT_FOUND.withDescription("solution " + id + " not found").asRuntimeException();
        }
        return solution;
    }

    @Override
    public GetAllTaskSolutionsResponse getAllTaskSolutions(GetAllTaskSolutionsRequest request) {
        return GetAllTaskSolutionsResponse.newBuilder()
                .addAllItems(solutions.values())
                .setPagination(TestData.pagination())
                .build();
    }
}

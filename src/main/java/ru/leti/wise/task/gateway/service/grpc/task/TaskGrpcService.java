package ru.leti.wise.task.gateway.service.grpc.task;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.task.TaskGrpc;
import ru.leti.wise.task.task.TaskGrpc.GetAllTaskRequest;
import ru.leti.wise.task.task.TaskGrpc.GetAllTaskResponse;
import ru.leti.wise.task.task.TaskGrpc.GetAllTaskSolutionsRequest;
import ru.leti.wise.task.task.TaskGrpc.GetAllTaskSolutionsResponse;
import ru.leti.wise.task.task.TaskOuterClass;
import ru.leti.wise.task.task.TaskOuterClass.Task;
import ru.leti.wise.task.task.TaskServiceGrpc.TaskServiceBlockingStub;

import java.util.List;

@Slf4j
@Component
@Observed
@RequiredArgsConstructor
public class TaskGrpcService {

    private final TaskServiceBlockingStub taskService;

    public Task getTask(String id) {
        log.debug("TaskService.getTask request, task id {}", id);
        var request = TaskGrpc.GetTaskRequest.newBuilder()
                .setId(id)
                .build();
        var task = taskService.getTask(request).getTask();
        log.debug("TaskService.getTask request, fetched task {}", task.getId());
        return task;
    }

    public List<TaskOuterClass.Task> getTasksByIds(List<String> taskIds) {
        log.debug("TaskService.getTasksByIds request, task ids {}", taskIds);
        var request = TaskGrpc.TaskIds.newBuilder()
                .addAllTaskIds(taskIds)
                .build();

        var tasks = taskService.getTasksByIds(request).getTasksList();
        log.debug("TaskService.getTasksByIds request, fetched {} tasks", tasks.size());
        return tasks;
    }

    public GetAllTaskResponse getAllTasks(GetAllTaskRequest request) {
        log.debug("TaskService.getAllTasks request, {}", request);
        var response = taskService.getAllTask(request);
        log.debug("TaskService.getAllTasks request, fetched {} tasks", response.getItemsCount());
        return response;
    }

    public void deleteTask(String id) {
        log.debug("TaskService.deleteTask request, task id {}", id);
        var request = TaskGrpc.DeleteTaskRequest.newBuilder()
                .setId(id)
                .build();

        taskService.deleteTask(request);
    }

    public Task createTask(Task task) {
        log.debug("TaskService.createTask request, task id {}", task.getId());
        var request = TaskGrpc.CreateTaskRequest.newBuilder()
                .setTask(task)
                .build();

        var created = taskService.createTask(request).getTask();
        log.debug("TaskService.createTask request, created task {}", created.getId());
        return created;
    }

    public Task updateTask(Task task) {
        log.debug("TaskService.updateTask request, task id {}", task.getId());
        var request = TaskGrpc.UpdateTaskRequest.newBuilder()
                .setTask(task)
                .build();

        var updated = taskService.updateTask(request).getTask();
        log.debug("TaskService.updateTask request, updated task {}", updated.getId());
        return updated;
    }

    public TaskOuterClass.Solution solveTask(TaskOuterClass.Solution solutionRequest) {
        log.debug("TaskService.solveTask request, solution id {}", solutionRequest.getId());
        var request = TaskGrpc.SolveTaskRequest.newBuilder()
                .setSolution(solutionRequest)
                .build();

        var solution = taskService.solveTask(request).getSolution();
        log.debug("TaskService.solveTask request, solved task solution {}", solution.getId());
        return solution;
    }

    public TaskOuterClass.Solution getTaskSolution(String id) {
        log.debug("TaskService.getTaskSolution request, solution id {}", id);
        var request = TaskGrpc.GetTaskSolutionRequest.newBuilder()
                .setId(id)
                .build();

        var solution = taskService.getTaskSolution(request).getSolution();
        log.debug("TaskService.getTaskSolution request, fetched task solution {}", solution.getId());
        return solution;
    }

    public GetAllTaskSolutionsResponse getAllTaskSolutions(GetAllTaskSolutionsRequest request) {
        log.debug("TaskService.getAllTaskSolutions request, {}", request);
        var response = taskService.getAllTaskSolutions(request);
        log.debug("TaskService.getAllTaskSolutions request, fetched {} task solutions", response.getItemsCount());
        return response;
    }
}

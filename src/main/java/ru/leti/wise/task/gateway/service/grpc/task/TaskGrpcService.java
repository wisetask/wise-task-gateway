package ru.leti.wise.task.gateway.service.grpc.task;

import com.google.protobuf.Empty;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.task.TaskGrpc;
import ru.leti.wise.task.task.TaskOuterClass;
import ru.leti.wise.task.task.TaskOuterClass.Task;
import ru.leti.wise.task.task.TaskServiceGrpc.TaskServiceBlockingStub;

import java.util.List;

@Component
@Observed
@RequiredArgsConstructor
public class TaskGrpcService {

    private final TaskServiceBlockingStub taskService;

    public Task getTask(String id) {
        var request = TaskGrpc.GetTaskRequest.newBuilder()
                .setId(id)
                .build();
        return taskService.getTask(request).getTask();
    }

    public List<Task> getAllTasks() {
        var request = Empty.newBuilder().build();

        return taskService.getAllTask(request).getTaskList();
    }

    public void deleteTask(String id) {
        var request = TaskGrpc.DeleteTaskRequest.newBuilder()
                .setId(id)
                .build();

        taskService.deleteTask(request);
    }

    public Task createTask(Task task) {
        var request = TaskGrpc.CreateTaskRequest.newBuilder()
                .setTask(task)
                .build();

        return taskService.createTask(request).getTask();
    }

    public Task updateTask(Task task) {
        var request = TaskGrpc.UpdateTaskRequest.newBuilder()
                .setTask(task)
                .build();

        return taskService.updateTask(request).getTask();
    }

    public TaskOuterClass.Solution solveTask(TaskOuterClass.Solution solutionRequest) {
        var request = TaskGrpc.SolveTaskRequest.newBuilder()
                .setSolution(solutionRequest)
                .build();

        return taskService.solveTask(request).getSolution();
    }

    public TaskOuterClass.Solution getTaskSolution(String id) {
        var request = TaskGrpc.GetTaskSolutionRequest.newBuilder()
                .setId(id)
                .build();

        return taskService.getTaskSolution(request).getSolution();
    }

    public List<TaskOuterClass.Solution> getAllTaskSolutions(String taskId, String authorId) {
        var request = TaskGrpc.GetAllTaskSolutionsRequest.newBuilder()
                .setTaskId(taskId)
                .setAuthorId(authorId)
                .build();

        return taskService.getAllTaskSolutions(request).getSolutionList();
    }

    public List<TaskOuterClass.Solution> getUserSolutionStatistic(String authorId) {
        var request = TaskGrpc.GetUserSolutionStatisticRequest.newBuilder()
                .setAuthorId(authorId)
                .build();

        return taskService.getUserSolutionStatistic(request).getSolutionList();
    }
}

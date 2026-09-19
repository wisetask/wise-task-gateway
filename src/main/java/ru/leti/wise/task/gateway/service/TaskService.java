package ru.leti.wise.task.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.PaginationMapper;
import ru.leti.wise.task.gateway.mapper.ProfileMapper;
import ru.leti.wise.task.gateway.mapper.SolutionMapper;
import ru.leti.wise.task.gateway.mapper.TaskMapper;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;
import ru.leti.wise.task.gateway.service.grpc.task.TaskGrpcService;
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
    private final TaskGrpcService taskGrpcService;
    private final ProfileGrpcService profileGrpcService;
    private final PaginationMapper paginationMapper;

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
        return toTask(taskGrpcService.getTask(id));
    }

    public Solution getTaskSolution(String id) {
        return toSolution(taskGrpcService.getTaskSolution(id));
    }

    public TaskGraph createTaskGraph(TaskGraphInput task, String authorId) {
        var grpcTask = taskGrpcService.createTask(taskMapper.toTaskGraph(task, authorId));
        return taskMapper.toTaskGraph(grpcTask, getGrpcProfile(grpcTask.getAuthorId()));
    }

    public TaskImplementation createTaskImplementation(TaskImplementationInput task, String authorId) {
        var grpcTask = taskGrpcService.createTask(taskMapper.toTaskImplementation(task, authorId));
        return taskMapper.toTaskImplementation(grpcTask, getGrpcProfile(grpcTask.getAuthorId()));
    }

    public TaskGraph updateTaskGraph(TaskGraphInput task, String authorId) {
        var grpcTask = taskGrpcService.updateTask(taskMapper.toTaskGraph(task, authorId));
        return taskMapper.toTaskGraph(grpcTask, getGrpcProfile(grpcTask.getAuthorId()));
    }

    public TaskImplementation updateTaskImplementation(TaskImplementationInput task, String authorId) {
        var grpcTask = taskGrpcService.updateTask(taskMapper.toTaskImplementation(task, authorId));
        return taskMapper.toTaskImplementation(grpcTask, getGrpcProfile(grpcTask.getAuthorId()));
    }

    public SolutionGraph solveTaskGraph(SolutionGraphInput solution, String authorId) {
        var grpcSolution = taskGrpcService.solveTask(solutionMapper.toSolutionGraph(solution, authorId));
        return solutionMapper.toSolutionGraph(
                grpcSolution,
                getTask(grpcSolution.getTaskId()),
                getProfile(grpcSolution.getAuthorId())
        );
    }

    public SolutionImplementation solveTaskImplementation(SolutionImplementationInput solution, String authorId) {
        var grpcSolution = taskGrpcService.solveTask(solutionMapper.toSolutionImplementation(solution, authorId));
        return solutionMapper.toSolutionImplementation(
                grpcSolution,
                getTask(grpcSolution.getTaskId()),
                getProfile(grpcSolution.getAuthorId())
        );
    }

    private Task toTask(TaskOuterClass.Task grpcTask) {
        return taskMapper.toTask(grpcTask, getGrpcProfile(grpcTask.getAuthorId()));
    }

    private Solution toSolution(TaskOuterClass.Solution grpcSolution) {
        return solutionMapper.toSolution(
                grpcSolution,
                getTask(grpcSolution.getTaskId()),
                getProfile(grpcSolution.getAuthorId())
        );
    }

    private List<Task> toTasks(List<TaskOuterClass.Task> grpcTasks) {
        var profiles = getProfilesByIds(grpcTasks.stream().map(TaskOuterClass.Task::getAuthorId).toList());

        return grpcTasks.stream()
                .map(grpcTask -> toTask(grpcTask, profiles))
                .toList();
    }

    private List<Solution> toSolutions(List<TaskOuterClass.Solution> grpcSolutions) {
        var profiles = getProfilesByIds(grpcSolutions.stream().map(TaskOuterClass.Solution::getAuthorId).toList());
        var tasks = getTasksByIds(grpcSolutions.stream().map(TaskOuterClass.Solution::getTaskId).toList());
        var taskProfiles = getProfilesByIds(tasks.values().stream().map(TaskOuterClass.Task::getAuthorId).toList());

        return grpcSolutions.stream()
                .map(grpcSolution -> solutionMapper.toSolution(
                        grpcSolution,
                        toTask(tasks.get(grpcSolution.getTaskId()), taskProfiles),
                        profileMapper.toProfile(profiles.get(grpcSolution.getAuthorId()))
                ))
                .toList();
    }

    private Task toTask(TaskOuterClass.Task grpcTask, Map<String, ProfileOuterClass.Profile> profiles) {
        if (grpcTask == null) {
            return null;
        }

        return taskMapper.toTask(grpcTask, profiles.get(grpcTask.getAuthorId()));
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
        return taskIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .map(taskGrpcService::getTask)
                .collect(Collectors.toMap(
                        TaskOuterClass.Task::getId,
                        task -> task
                ));
    }
}

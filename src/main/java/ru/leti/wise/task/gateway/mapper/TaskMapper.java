package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.*;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.profile.ProfileOuterClass;
import ru.leti.wise.task.task.TaskGrpc;
import ru.leti.wise.task.task.TaskGrpc.GetAllTaskRequest;
import ru.leti.wise.task.task.TaskGrpc.TaskFilter;
import ru.leti.wise.task.task.TaskOuterClass;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {GraphMapper.class, PluginMapper.class, ProfileMapper.class})
public interface TaskMapper {

    TaskFilter toTaskFilter(TaskFilterInput filter);
    GetAllTaskRequest toGetAllRequest(GetAllTaskRequestInput request);

    @Named("toTask")
    default Task toTask(TaskOuterClass.Task task, ProfileOuterClass.Profile profile) {
        if (task.hasTaskGraph()) {
            return toTaskGraph(task, profile);
        } else {
            return toTaskImplementation(task, profile);
        }
    }


    @Mapping(target = "taskImplementation", ignore = true)
    @Mapping(target = "taskGraph", source = ".")
    @Mapping(target = "authorId", expression = "java(authorId)")
    TaskOuterClass.Task toTaskGraph(TaskGraphInput taskGraph, @Context String authorId);


    @Mapping(target = "taskGraph", ignore = true)
    @Mapping(target = "taskImplementation", source = ".")
    @Mapping(target = "authorId", expression = "java(authorId)")
    TaskOuterClass.Task toTaskImplementation(TaskImplementationInput taskImplementation, @Context String authorId);

    @Mapping(target = ".", source = "task.taskGraph")
    @Mapping(target = "id", source = "task.id")
    @Mapping(target = "author", source = "profile")
    TaskGraph toTaskGraph(TaskOuterClass.Task task, ProfileOuterClass.Profile profile);

    @Mapping(target = ".", source = "task.taskImplementation")
    @Mapping(target = "id", source = "task.id")
    @Mapping(target = "author", source = "profile")
    TaskImplementation toTaskImplementation(TaskOuterClass.Task task, ProfileOuterClass.Profile profile);

    @Mapping(target = "plugin", ignore = true)
    PluginInfo toPluginInfo(TaskOuterClass.PluginInfo pluginInfo);

    default TaskOuterClass.TaskType toTaskType(TaskType taskType) {
        return TaskOuterClass.TaskType.valueOf(taskType.name());
    }

    default TaskOuterClass.PluginType toPluginType(PluginType pluginType) {
        return TaskOuterClass.PluginType.valueOf(pluginType.name());
    }

    default PluginType toPluginType(TaskOuterClass.PluginType pluginType) {
        return PluginType.valueOf(pluginType.name());
    }
}

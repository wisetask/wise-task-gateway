package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.*;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.task.TaskOuterClass;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {GraphMapper.class, PluginMapper.class})
public interface TaskMapper {


    @Named("toTask")
    default Task toTask(TaskOuterClass.Task task) {
        if (task.hasTaskGraph()) {
            return toTaskGraph(task);
        } else if (task.hasTaskImplementation()) {
            return toTaskImplementation(task);
        }
        throw new RuntimeException("123"); //TODO: поправить ошибки, которые никогда не возникнут
    }

    @IterableMapping(qualifiedByName = "toTask")
    List<Task> toTasks(List<TaskOuterClass.Task> tasks);

    @Mapping(target = "taskImplementation", ignore = true)
    @Mapping(target = "taskGraph", source = ".")
    @Mapping(target = "authorId", expression = "java(authorId)")
    TaskOuterClass.Task toTaskGraph(TaskGraphInput taskGraph, @Context String authorId);


    @Mapping(target = "taskGraph", ignore = true)
    @Mapping(target = "taskImplementation", source = ".")
    @Mapping(target = "authorId", expression = "java(authorId)")
    TaskOuterClass.Task toTaskImplementation(TaskImplementationInput taskImplementation, @Context String authorId);

    @Mapping(target = ".", source = "task.taskGraph")
    TaskGraph toTaskGraph(TaskOuterClass.Task task);

    @Mapping(target = ".", source = "task.taskImplementation")
    TaskImplementation toTaskImplementation(TaskOuterClass.Task task);

    default TaskOuterClass.TaskType toTaskType(TaskType taskType) {
        return TaskOuterClass.TaskType.valueOf(taskType.name());
    }

    default TaskOuterClass.PluginType toPluginType(PluginType pluginType) {
        return TaskOuterClass.PluginType.valueOf(pluginType.name());
    }
}

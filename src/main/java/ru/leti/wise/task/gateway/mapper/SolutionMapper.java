package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.*;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.task.TaskGrpc;
import ru.leti.wise.task.task.TaskOuterClass;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        uses = {GraphMapper.class, PluginMapper.class})
public interface SolutionMapper {

    TaskGrpc.GetAllTaskSolutionsRequest toGetAllRequest(GetAllTaskSolutionsRequestInput request);

    default Solution toSolution(TaskOuterClass.Solution solution, Task task, Profile author) {
        if (solution.hasSolutionGraph()) {
            return toSolutionGraph(solution, task, author);
        } else if (solution.hasSolutionImplementation()) {
            return toSolutionImplementation(solution, task, author);
        }
        throw new IllegalArgumentException("Решение должно содержать граф или реализацию");
    }

    default SolutionGraph toSolutionGraph(TaskOuterClass.Solution solution, Task task, Profile author) {
        var solutionGraph = toSolutionGraph(solution);
        solutionGraph.setTask(task);
        solutionGraph.setAuthor(author);

        return solutionGraph;
    }

    default SolutionImplementation toSolutionImplementation(TaskOuterClass.Solution solution, Task task, Profile author) {
        var solutionImplementation = toSolutionImplementation(solution);
        solutionImplementation.setTask(task);
        solutionImplementation.setAuthor(author);

        return solutionImplementation;
    }

    @Mapping(target = "solutionImplementation", ignore = true)
    @Mapping(target = "authorId", expression = "java(authorId)")
    @Mapping(target = "isCorrect", ignore = true)
    TaskOuterClass.Solution toSolutionGraph(SolutionGraphInput solution, @Context String authorId);

    @Mapping(target = "solutionGraph", ignore = true)
    @Mapping(target = "solutionImplementation.code", source = "solution.code")
    @Mapping(target = "authorId", expression = "java(authorId)")
    @Mapping(target = "isCorrect", ignore = true)
    TaskOuterClass.Solution toSolutionImplementation(SolutionImplementationInput solution, @Context String authorId);

    @Mapping(target = ".", source = "solution.solutionGraph")
    SolutionGraph toSolutionGraph(TaskOuterClass.Solution solution);

    @Mapping(target = ".", source = "solution.solutionImplementation")
    SolutionImplementation toSolutionImplementation(TaskOuterClass.Solution solution);

}

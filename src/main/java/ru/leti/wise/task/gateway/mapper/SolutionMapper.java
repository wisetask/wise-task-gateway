package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.*;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.task.TaskGrpc;
import ru.leti.wise.task.task.TaskOuterClass;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        uses = {GraphMapper.class, PluginMapper.class})
public interface SolutionMapper {

    TaskGrpc.GetAllTaskSolutionsRequest toGetAllRequest(GetAllTaskSolutionsRequestInput request);

    default Solution toSolution(TaskOuterClass.Solution solution) {
        if (solution.hasSolutionGraph()) {
            return toSolutionGraph(solution);
        } else if (solution.hasSolutionImplementation()) {
            return toSolutionImplementation(solution);
        }
        throw new IllegalArgumentException("Решение должно содержать граф или реализацию");
    }

    @Mapping(target = "solutionImplementation", ignore = true)
    @Mapping(target = "authorId", expression = "java(authorId)")
    @Mapping(target = "solutionGraph.graph", source = "solution.graph", qualifiedByName = "graphWithoutAuthor")
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
    @Mapping(target = "implementationResult", source = "solution.solutionImplementation.implementationResultList")
    SolutionImplementation toSolutionImplementation(TaskOuterClass.Solution solution);

    @Mapping(target = "plugin", ignore = true)
    PluginResult toPluginResult(TaskOuterClass.PluginResult pluginResult);

}

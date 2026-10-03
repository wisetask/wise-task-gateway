package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.*;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.plugin.PluginGrpc;
import ru.leti.wise.task.plugin.PluginGrpc.PluginFilter;
import ru.leti.wise.task.plugin.PluginOuterClass;
import ru.leti.wise.task.profile.ProfileOuterClass;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        uses = {GraphMapper.class, ProfileMapper.class})
public interface PluginMapper {

    PluginFilter toPluginFilter(PluginFilterInput filter);

    PluginGrpc.GetAllPluginRequest toGetAllRequest(GetAllPluginRequestInput request);

    @Mapping(target = "id", source = "plugin.id")
    @Mapping(target = "author", source = "profile")
    Plugin toPlugin(PluginOuterClass.Plugin plugin, ProfileOuterClass.Profile profile);

    @Mapping(target = "authorId", expression = "java(authorId)")
    PluginOuterClass.Plugin toPlugin(PluginInput plugin, @Context String authorId);

    ImplementationResult toImplementationResult(PluginOuterClass.ImplementationResult implementationResult);

    @Mapping(target = "graph", ignore = true)
    GraphTestResult toGraphTestResult(PluginOuterClass.GraphTestResult graphTestResult);

    @Mapping(target = "graph", source = "solution.payload.graph", qualifiedByName = "graphWithoutAuthor")
    @Mapping(target = "otherGraph", source = "solution.additionalPayload.otherGraph", qualifiedByName = "graphWithoutAuthor")
    @Mapping(target = "handwrittenAnswer", source = "solution.additionalPayload.handwrittenAnswer")
    PluginOuterClass.Solution toSolution(SolutionInput solution);


    default PluginOuterClass.PluginType toPluginType(PluginType pluginType) {
        return PluginOuterClass.PluginType.valueOf(pluginType.name());
    }

    default PluginType toPluginType(PluginOuterClass.PluginType pluginType) {
        return PluginType.valueOf(pluginType.name());
    }

    default PluginOuterClass.GraphType toPluginType(GraphType graphType) {
        return PluginOuterClass.GraphType.valueOf(graphType.name());
    }

    default GraphType toPluginType(PluginOuterClass.GraphType graphType) {
        return GraphType.valueOf(graphType.name());
    }
}

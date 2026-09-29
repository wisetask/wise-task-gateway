package ru.leti.wise.task.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.service.GraphService;
import ru.leti.wise.task.gateway.utils.SecurityUtils;


@Controller
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\", \"ADMIN\")")
    @QueryMapping
    public Graph getGraphById(@Argument String id) {
        return graphService.getGraphById(id);
    }

    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\", \"ADMIN\")")
    @QueryMapping
    public GetAllGraphsResponse getAllGraphs(@Argument GetAllGraphsRequestInput request) {
        return graphService.getAllGraphsResponse(request);
    }

    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\", \"ADMIN\")")
    @MutationMapping
    public Graph createGraph(@Argument GraphInput graph) {
        return graphService.createGraph(graph, SecurityUtils.getUserId());
    }

    @PreAuthorize("hasAnyRole(\"USER\", \"AUTHOR\", \"ADMIN\")")
    @MutationMapping
    public Graph generateGraph(@Argument GenerateGraphRequest generateGraphRequest) {
        return graphService.generateGraph(generateGraphRequest, SecurityUtils.getUserId());
    }

    @PreAuthorize(
            "hasRole('ADMIN') or " +
                    "(hasAnyRole('USER', 'AUTHOR') and " +
                    "@graphService.isOwnerGraph(authentication.principal.id, #id))")
    @MutationMapping
    public String deleteGraph(@Argument String id) {
        return graphService.deleteGraph(id);
    }
}

package ru.leti.wise.task.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.GraphMapper;
import ru.leti.wise.task.gateway.service.grpc.graph.GraphGrpcService;

import java.util.List;


@Controller
@RequiredArgsConstructor
public class GraphController {

    private final GraphMapper graphMapper;
    private final GraphGrpcService graphGrpcService;


    @PreAuthorize("hasAnyRole(\"USER\",\"AUTHOR\",\"ADMIN\")")
    @MutationMapping
    public Graph createGraph(@Argument GraphInput graph) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = ((User) auth.getPrincipal()).getUsername();
        return graphMapper.toGraph(graphGrpcService.createGraph(graph, userId));
    }

    @PreAuthorize("hasAnyRole(\"USER\",\"AUTHOR\",\"ADMIN\")")
    @MutationMapping
    public Graph generateGraph(@Argument GenerateGraphRequest generateGraphRequest) {
        return graphMapper.toGraph(graphGrpcService.generateGraph(generateGraphRequest));
    }


    @PreAuthorize("hasAnyRole(\"USER\",\"AUTHOR\",\"ADMIN\")")
    @QueryMapping
    public Graph getGraphById(@Argument String id) {
        return graphMapper.toGraph(graphGrpcService.getGraphById(id));
    }


    @PreAuthorize("hasAnyRole(\"USER\",\"AUTHOR\",\"ADMIN\")")
    @QueryMapping
    public List<Graph> getGraphLibrary() {
        return graphMapper.toGraphs(graphGrpcService.getGraphLibrary());
    }

    @PreAuthorize(
            "hasRole('ADMIN') or " +
                    "(hasAnyRole('USER', 'AUTHOR') and " +
                    "@graphGrpcService.isOwnerGraph(authentication.principal.id, #id))"
    )

    @MutationMapping
    public String deleteGraph(@Argument String id) {
        return graphGrpcService.deleteGraph(id);
    }
}

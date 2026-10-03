package ru.leti.wise.task.gateway.controller;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.GetAllProfilesRequestInput;
import ru.leti.graphql.types.GetAllProfilesResponse;
import ru.leti.graphql.types.Profile;
import ru.leti.graphql.types.ProfileInput;
import ru.leti.wise.task.gateway.mapper.ProfileMapper;
import ru.leti.wise.task.gateway.metrics.GraphQlMetrics;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;

@Slf4j
@Observed
@Controller
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileGrpcService profileGrpcService;
    private final ProfileMapper profileMapper;
    private final GraphQlMetrics metrics;


    @QueryMapping
    @PreAuthorize("hasAnyRole(\"USER\",\"AUTHOR\",\"ADMIN\")")
    public GetAllProfilesResponse getAllProfiles(
            @Argument GetAllProfilesRequestInput request
    ) {
        metrics.increment("getAllProfiles");
        var grpcRequest = profileMapper.toGetAllRequest(request);
        var grpcResponse = profileGrpcService.getAllProfiles(grpcRequest);
        return profileMapper.toGetAllResponse(grpcResponse);
    }


    @QueryMapping
    @PreAuthorize("hasAnyRole(\"USER\",\"AUTHOR\",\"ADMIN\")")
    public Profile getProfile(@Argument String id) {
        metrics.increment("getProfile");
        return profileMapper.toProfile(profileGrpcService.getProfile(id));
    }

    @MutationMapping
    @PreAuthorize("hasRole(\"ADMIN\")")
    public Profile updateProfile(@Argument ProfileInput profile) {
        metrics.increment("updateProfile");
        return profileMapper.toProfile(profileGrpcService.updateProfile(profileMapper.toProfile(profile)));
    }

    @MutationMapping
    @PreAuthorize("authentication.principal.id.equals(#id) or hasRole(\"ADMIN\")")
    public String deleteProfile(@Argument String id) {
        metrics.increment("deleteProfile");
        profileGrpcService.deleteProfile(id);
        return id;
    }
}

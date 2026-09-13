package ru.leti.wise.task.gateway.controller;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.ProfileMapper;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;

import java.util.List;

@Slf4j
@Observed
@Controller
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileGrpcService profileGrpcService;
    private final ProfileMapper profileMapper;


    @QueryMapping
    @PreAuthorize("hasAnyRole(\"USER\",\"AUTHOR\",\"ADMIN\")")
    public List<Profile> getAllProfiles() {
        return profileMapper.toProfiles(profileGrpcService.getAllProfiles());
    }


    @QueryMapping
    @PreAuthorize("hasAnyRole(\"USER\",\"AUTHOR\",\"ADMIN\")")
    public Profile getProfile(@Argument String id) {
        return profileMapper.toProfile(profileGrpcService.getProfile(id));
    }

    @MutationMapping
    @PreAuthorize("hasRole(\"ADMIN\")")
    public Profile updateProfile(@Argument ProfileInput profile) {
        return profileMapper.toProfile(profileGrpcService.updateProfile(profileMapper.toProfile(profile)));
    }

    @MutationMapping
    @PreAuthorize("authentication.principal.id.equals(#id) or hasRole(\"ADMIN\")")
    public String deleteProfile(@Argument String id) {
        profileGrpcService.deleteProfile(id);
        return id;
    }
}

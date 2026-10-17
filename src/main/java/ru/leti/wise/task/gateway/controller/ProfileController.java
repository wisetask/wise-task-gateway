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
import ru.leti.wise.task.gateway.metrics.GraphQlMetrics;
import ru.leti.wise.task.gateway.service.ProfileService;
import ru.leti.wise.task.gateway.utils.SecurityUtils;

@Slf4j
@Observed
@Controller
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;
    private final GraphQlMetrics metrics;

    @QueryMapping
    @PreAuthorize("hasAnyRole(\"USER\",\"AUTHOR\",\"ADMIN\")")
    public Profile getMyProfile(){
        metrics.increment("getMyProfile");
        var userId = SecurityUtils.getUserId();
        return profileService.getProfile(userId);
    }

    @QueryMapping
    @PreAuthorize("""
            hasAnyRole("USER", "AUTHOR", "ADMIN")
            """)
    public GetAllProfilesResponse getAllProfiles(
            @Argument GetAllProfilesRequestInput request
    ) {
        metrics.increment("getAllProfiles");
        return profileService.getAllProfiles(request);
    }


    @QueryMapping
    @PreAuthorize("""
            hasAnyRole("USER", "AUTHOR", "ADMIN")
            """)
    public Profile getProfile(@Argument String id) {
        metrics.increment("getProfile");
        return profileService.getProfile(id);
    }

    @MutationMapping
    @PreAuthorize("""
            hasRole("ADMIN")
            """)
    public Profile updateProfile(@Argument ProfileInput profile) {
        metrics.increment("updateProfile");
        return profileService.updateProfile(profile);
    }

    @MutationMapping
    @PreAuthorize("""
            authentication.principal.id.equals(#id) or
            hasRole("ADMIN")
            """)
    public String deleteProfile(@Argument String id) {
        metrics.increment("deleteProfile");
        return profileService.deleteProfile(id);
    }
}

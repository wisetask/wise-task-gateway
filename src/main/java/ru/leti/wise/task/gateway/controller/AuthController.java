package ru.leti.wise.task.gateway.controller;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.service.SecurityService;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;

@Slf4j
@Observed
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final SecurityService securityService;
    private final ProfileGrpcService profileGrpcService;

    @MutationMapping
    @PreAuthorize("isAnonymous()")
    public Token signIn(@Argument SignInRequest signInRequest) {
        return securityService.signIn(signInRequest);
    }

    @MutationMapping
    @PreAuthorize("isAnonymous()")
    public Token signUp(@Argument SignUpRequest signUpRequest) {
        return securityService.signUp(signUpRequest);
    }

    @MutationMapping
    @PreAuthorize("isAnonymous()")
    public Token resetPassword(@Argument ResetPasswordRequest resetPasswordRequest) {
        return securityService.resetPassword(resetPasswordRequest);
    }

    @MutationMapping
    @PreAuthorize("isAnonymous()")
    public String sendResetPasswordEmail(@Argument String email) {
        profileGrpcService.sendResetPasswordEmail(email);
        return email;
    }

    @MutationMapping
    @PreAuthorize("authentication.principal.id.equals(#id) or hasRole(\"ADMIN\")")
    public String changePassword(@Argument String id, @Argument String oldPassword, @Argument String newPassword) {
        profileGrpcService.changePassword(id, oldPassword, newPassword);
        return id;
    }
}

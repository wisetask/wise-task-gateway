package ru.leti.wise.task.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import ru.leti.wise.task.gateway.configuration.JwtProperties;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.gateway.mapper.ProfileMapper;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;
import ru.leti.wise.task.gateway.utils.SecurityUtils;
import ru.leti.wise.task.profile.ProfileOuterClass;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {

    private final ProfileGrpcService profileGrpcService;
    private final ProfileMapper profileMapper;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public Token signIn(SignInRequest request) {
        var profile = profileGrpcService.signIn(request.getEmail(), request.getPassword());
        log.info("signIn request, signed in profile {}", profile.getId());
        return new Token(
                generateAccessToken(profile),
                generateRefreshToken(profile)
        );
    }

    public Token signUp(SignUpRequest request) {
        log.debug("signUp request, {}", request.getProfile().getEmail());
        var profile = profileGrpcService.signUp(profileMapper.toProfile(request.getProfile()), false);
        log.info("signUp request, signed up profile {}", profile.getId());
        return new Token(
                generateAccessToken(profile),
                generateRefreshToken(profile)
        );
    }

    public Token resetPassword(ResetPasswordRequest request){
        log.debug("resetPassword request, {}", SecurityUtils.getUserIdOrAnonymous());
        var profile = profileGrpcService.resetPassword(request.getRecoveryToken(), request.getNewPassword());
        log.info("resetPassword request, reset password for profile {}", profile.getId());
        return new Token(
                generateAccessToken(profile),
                generateRefreshToken(profile)
        );
    }

    private String generateAccessToken(ProfileOuterClass.Profile user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("wise-task")
                .subject(user.getId())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.accessExpiresAt()))
                .claim("email", user.getEmail())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String generateRefreshToken(ProfileOuterClass.Profile user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("wise-task")
                .subject(user.getId())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.refreshExpiresAt()))
                .claim("type", "refresh")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

}
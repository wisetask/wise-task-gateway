package ru.leti.wise.task.gateway.configuration.security.converters;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.gateway.dto.OAuth2Issuers;
import ru.leti.wise.task.gateway.dto.UserCredentials;
import ru.leti.wise.task.gateway.exception.JwtConverterException;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;
import ru.leti.wise.task.profile.ProfileOuterClass;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GoogleJwtConverterMatcher implements JwtConverterMatcher {

    private final ProfileGrpcService profileGrpcService;

    @Override
    public boolean match(Jwt jwt) {
        if (jwt == null)
            return false;
        var iss = jwt.getIssuer();
        return iss != null && iss.toExternalForm().equals(OAuth2Issuers.GOOGLE.getIss());
    }

    @Override
    public UserCredentials convert(Jwt jwt) {
        var iss = jwt.getIssuer();
        if (iss == null) {
            throw new JwtConverterException("Отсутствует Issuer claim");
        }
        var issStr = iss.toExternalForm();
        if (issStr.equals(OAuth2Issuers.GOOGLE.getIss())) {
            var email = jwt.getClaimAsString("email");

            ProfileOuterClass.Profile profile;
            try {
                profile = profileGrpcService.getProfileByEmail(email);
            } catch (StatusRuntimeException e) {
                if (!e.getStatus().getCode().equals(Status.NOT_FOUND.getCode())) {
                    throw e;
                }
                profile = signUpProfile(jwt);
            }
            var role = profile.getProfileRole().name();
            var id = profile.getId();
            var grantedAuthorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            return new UserCredentials(id, email, role, grantedAuthorities);
        }
        throw new JwtConverterException("JWT issuer не является Google");
    }


    public ProfileOuterClass.Profile signUpProfile(Jwt jwt) {
        var email = jwt.getClaimAsString("email");
        var firstName = jwt.getClaimAsString("given_name");
        var lastName = jwt.getClaimAsString("family_name");
        if (email == null || firstName == null || lastName == null) {
            throw new JwtConverterException(
                    "Токен идентификации не содержит основных 'claim' для создания нового пользователя"
            );
        }
        var builder = ProfileOuterClass.Profile.newBuilder();
        builder.setEmail(email);
        builder.setFirstName(firstName);
        builder.setLastName(lastName);
        builder.setProfileRole(ProfileOuterClass.Role.USER);
        return profileGrpcService.signUp(builder.build(), true);
    }
}

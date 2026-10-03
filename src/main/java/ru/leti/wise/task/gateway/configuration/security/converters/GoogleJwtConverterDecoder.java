package ru.leti.wise.task.gateway.configuration.security.converters;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.gateway.configuration.OAuth2IssuerProperties;
import ru.leti.wise.task.gateway.dto.UserCredentials;
import ru.leti.wise.task.gateway.exception.JwtIssuerException;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;
import ru.leti.wise.task.profile.ProfileOuterClass;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GoogleJwtConverterDecoder implements JwtConverterDecoder {

    private final ProfileGrpcService profileGrpcService;
    private final OAuth2IssuerProperties oAuth2Properties;

    @Override
    public boolean match(String issuer) {
        return issuer != null && issuer.equals(oAuth2Properties.googleIssuer());
    }


    @Override
    public UserCredentials convert(Jwt jwt) {
        var iss = jwt.getIssuer();
        if (iss == null) {
            throw new JwtIssuerException("Отсутствует Issuer claim");
        }
        var issStr = iss.toExternalForm();
        if (issStr.equals(oAuth2Properties.googleIssuer())) {
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
        throw new JwtIssuerException("JWT issuer не является Google");
    }

    public JwtDecoder decoder(){
        return JwtDecoders.fromIssuerLocation(oAuth2Properties.googleIssuer());
    }

    public ProfileOuterClass.Profile signUpProfile(Jwt jwt) {
        var email = jwt.getClaimAsString("email");
        var firstName = jwt.getClaimAsString("given_name");
        var lastName = jwt.getClaimAsString("family_name");
        if (email == null || firstName == null || lastName == null) {
            throw new JwtIssuerException(
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

package ru.leti.wise.task.gateway.configuration.security.converters;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.gateway.configuration.JwtProperties;
import ru.leti.wise.task.gateway.dto.UserCredentials;
import ru.leti.wise.task.gateway.exception.JwtIssuerException;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;
import ru.leti.wise.task.profile.ProfileOuterClass;

import java.util.List;

@Component
@Configuration
@RequiredArgsConstructor
public class WiseTaskJwtConverterDecoder implements JwtConverterDecoder {
    private final JwtProperties jwtProperties;
    private final JwtDecoder jwtInternalDecoder;
    private final ProfileGrpcService profileGrpcService;

    @Override
    public boolean match(String issuer) {
        return issuer != null && issuer.equals(jwtProperties.issuer());
    }

    @Override
    public JwtDecoder decoder() {
        return jwtInternalDecoder;
    }

    @Override
    public UserCredentials convert(Jwt jwt) {
        var iss = jwt.getClaimAsString("iss");
        if (iss == null) {
            throw new JwtIssuerException("Отсутствует Issuer claim");
        }
        if (iss.equals(jwtProperties.issuer())) {
            var email = jwt.getClaimAsString("email");
            ProfileOuterClass.Profile profile = profileGrpcService.getProfileByEmail(email);;
            var role = profile.getProfileRole().name();
            var id = profile.getId();
            var grantedAuthorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            return new UserCredentials(id, email, role, grantedAuthorities);
        }
        throw new JwtIssuerException("JWT issuer не является WiseTask");
    }
}

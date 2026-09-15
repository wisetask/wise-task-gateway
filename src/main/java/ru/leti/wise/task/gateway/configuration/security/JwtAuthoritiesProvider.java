package ru.leti.wise.task.gateway.configuration.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.gateway.dto.OAuth2Issuers;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthoritiesProvider
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final ProfileGrpcService profileGrpcService;

    private final List<JwtConverterMatcher>

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        var issuer = jwt.getIssuer();
        if (issuer == null) {
            return List.of();
        }
        if (issuer.equals(OAuth2Issuers.GOOGLE.getIss())) {
            var email = jwt.getClaim("email");
            if (email == null) {
                return List.of();
            }
            var profile = profileGrpcService.getProfileByEmail(email.toString());
            var role = profile.getProfileRole().name();
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return List.of();
    }
}
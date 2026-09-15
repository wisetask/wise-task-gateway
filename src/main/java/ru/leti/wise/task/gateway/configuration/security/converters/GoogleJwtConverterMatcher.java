package ru.leti.wise.task.gateway.configuration.security.converters;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.gateway.dto.OAuth2Issuers;
import ru.leti.wise.task.gateway.dto.UserCredentials;
import ru.leti.wise.task.gateway.exception.JwtConverterException;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GoogleJwtConverterMatcher implements JwtConverterMatcher{

    private final ProfileGrpcService profileGrpcService;

    @Override
    public boolean match(Jwt jwt) {
        if(jwt == null)
            return false;
        var iss = jwt.getIssuer();
        return iss != null && iss.toExternalForm().equals(OAuth2Issuers.GOOGLE.getIss());
    }

    @Override
    public UserCredentials convert(Jwt jwt) {
        var iss = jwt.getIssuer();
        if(iss == null){
            throw new JwtConverterException("Отсутствует Issuer claim");
        }
        var issStr = iss.toExternalForm();
        if(issStr.equals(OAuth2Issuers.GOOGLE.getIss())){
            var email = jwt.getClaimAsString("email");
            var profile = profileGrpcService.getProfileByEmail(email);
            var role = profile.getProfileRole().name();
            var id = profile.getId();
            var grantedAuthorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            return new UserCredentials(id, email, role, grantedAuthorities);
        }
        throw new JwtConverterException("JWT issuer не является Google");
    }
}

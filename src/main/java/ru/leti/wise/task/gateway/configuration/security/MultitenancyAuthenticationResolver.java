package ru.leti.wise.task.gateway.configuration.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.gateway.configuration.security.converters.JwtUserCredentialsConverter;
import ru.leti.wise.task.gateway.exception.IssuerException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MultitenancyAuthenticationResolver implements AuthenticationManagerResolver<String> {

    @Value("${app.oauth2.trusted-issuers}")
    private List<String> trustedIssuers;

    private final Map<String, AuthenticationManager> managers = new ConcurrentHashMap<>();
    private final JwtUserCredentialsConverter converter;

    @Override
    public AuthenticationManager resolve(String issuer) {
        if (!trustedIssuers.contains(issuer)) {
            throw new IssuerException(issuer);
        }
        return managers.computeIfAbsent(issuer, iss -> {
            JwtDecoder decoder = JwtDecoders.fromIssuerLocation(iss);
            JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
            provider.setJwtAuthenticationConverter(converter);
            return provider::authenticate;
        });
    }
}

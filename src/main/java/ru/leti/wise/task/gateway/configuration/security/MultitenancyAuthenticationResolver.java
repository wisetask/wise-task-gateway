package ru.leti.wise.task.gateway.configuration.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.gateway.configuration.security.converters.JwtUserCredentialsProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MultitenancyAuthenticationResolver implements AuthenticationManagerResolver<String> {


    private final Map<String, AuthenticationManager> managers = new ConcurrentHashMap<>();
    private final JwtUserCredentialsProvider provider;


    @Override
    public AuthenticationManager resolve(String issuer) {
        return managers.computeIfAbsent(issuer, iss -> {
            var decoder = provider.getDecoder(iss);
            JwtAuthenticationProvider authProvider = new JwtAuthenticationProvider(decoder);
            var converter = provider.getConverter(iss);
            authProvider.setJwtAuthenticationConverter(converter);
            return authProvider::authenticate;
        });
    }
}

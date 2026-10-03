package ru.leti.wise.task.gateway.configuration.security.converters;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.gateway.exception.JwtIssuerException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtUserCredentialsProvider {

    private final List<JwtConverterDecoder> converterMatchers;

    public Converter<Jwt, AbstractAuthenticationToken> getConverter(String issuer) {
        for (var converterMatcher : converterMatchers) {
            if (converterMatcher.match(issuer)) {
                return converterMatcher;
            }
        }
        throw new JwtIssuerException("Не удалось найти соответствующего конвертера для '%s'.".formatted(issuer));
    }

    public JwtDecoder getDecoder(String issuer) {
        for(var converterMatcher : converterMatchers) {
            if(converterMatcher.match(issuer)) {
                return converterMatcher.decoder();
            }
        }
        throw new JwtIssuerException("Не удалось найти соответствующего декодера '%s'.".formatted(issuer));
    }
}
package ru.leti.wise.task.gateway.configuration.security.converters;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.gateway.dto.UserCredentials;
import ru.leti.wise.task.gateway.exception.JwtConverterException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtUserCredentialsConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final List<JwtConverterMatcher> converterMatchers;

    @Override
    public UserCredentials convert(Jwt jwt) {
        for(var converterMatcher : converterMatchers){
            if(converterMatcher.match(jwt)){
                return converterMatcher.convert(jwt);
            }
        }
        throw new JwtConverterException("Не удалось найти соответствующего провайдера");
    }
}
package ru.leti.wise.task.gateway.configuration.security.converters;

import org.springframework.security.oauth2.jwt.Jwt;
import ru.leti.wise.task.gateway.dto.UserCredentials;

public interface JwtConverterMatcher {

    boolean match(Jwt jwt);

    UserCredentials convert(Jwt jwt);
}

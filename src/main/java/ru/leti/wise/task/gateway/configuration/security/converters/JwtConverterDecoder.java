package ru.leti.wise.task.gateway.configuration.security.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import ru.leti.wise.task.gateway.dto.UserCredentials;

public interface JwtConverterDecoder extends Converter<Jwt, AbstractAuthenticationToken> {

    boolean match(String issuer);
    JwtDecoder decoder();
}

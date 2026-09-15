package ru.leti.wise.task.gateway.configuration.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

public interface JwtConverterMatcher {

    boolean match();

    Collection<GrantedAuthority> convert(Jwt jwt);
}

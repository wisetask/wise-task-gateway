package ru.leti.wise.task.gateway.utils;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;


public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((Jwt) authentication.getPrincipal()).getSubject();
    }

    public static String getUserIdOrAnonymous() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return "anonymous";
        }

        return jwt.getSubject();
    }
}

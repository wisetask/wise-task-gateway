package ru.leti.wise.task.gateway.utils;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.leti.wise.task.gateway.dto.UserCredentials;


public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((UserCredentials) authentication.getPrincipal()).getId();
    }

    public static String getUserIdOrAnonymous() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserCredentials user)) {
            return "anonymous";
        }

        return user.getId();
    }
}

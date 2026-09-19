package ru.leti.wise.task.gateway.utils;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;


public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((User) authentication.getPrincipal()).getUsername();
    }
}

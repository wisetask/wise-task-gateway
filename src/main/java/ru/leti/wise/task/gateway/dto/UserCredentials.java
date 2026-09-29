package ru.leti.wise.task.gateway.dto;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
public class UserCredentials extends AbstractAuthenticationToken {

    private final String id;
    private final String email;
    private final String role;

    public UserCredentials(
            String id,
            String email,
            String role,
            @Nullable Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.role = role;
        this.email = email;
        super(authorities);
    }

    @Override
    public @Nullable Object getCredentials() {
        return this;
    }

    @Override
    public @Nullable Object getPrincipal() {
        return this;
    }
}
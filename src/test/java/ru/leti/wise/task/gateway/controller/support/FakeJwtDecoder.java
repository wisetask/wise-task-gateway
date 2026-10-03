package ru.leti.wise.task.gateway.controller.support;

import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.HashMap;
import java.util.Map;

/**
 * Фейковый декодер внутренних JWT: отдаёт заранее подготовленные токены,
 * на неизвестный токен бросает {@link BadJwtException}, как настоящий NimbusJwtDecoder.
 */
public class FakeJwtDecoder implements JwtDecoder {

    public static final String WISE_TASK_ISSUER = "wise-task";

    private final Map<String, Jwt> tokens = new HashMap<>();

    public void putRefreshToken(String tokenValue, String userId) {
        putToken(builder(tokenValue, WISE_TASK_ISSUER, userId)
                .claim("type", "refresh")
                .build());
    }

    public void putAccessToken(String tokenValue, String userId, String email) {
        putToken(builder(tokenValue, WISE_TASK_ISSUER, userId)
                .claim("email", email)
                .build());
    }

    public void putTokenOfAnotherIssuer(String tokenValue, String issuer, String userId) {
        putToken(builder(tokenValue, issuer, userId)
                .claim("type", "refresh")
                .build());
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        var jwt = tokens.get(token);
        if (jwt == null) {
            throw new BadJwtException("Неизвестный токен " + token);
        }
        return jwt;
    }

    private Jwt.Builder builder(String tokenValue, String issuer, String userId) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuer(issuer)
                .subject(userId);
    }

    private void putToken(Jwt jwt) {
        tokens.put(jwt.getTokenValue(), jwt);
    }
}

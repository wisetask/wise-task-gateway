package ru.leti.wise.task.gateway.controller.support;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Фейковый кодировщик внутренних JWT: выдаёт уникальное значение токена на каждый вызов
 * и запоминает claims, чтобы тесты могли проверить содержимое выданных токенов.
 */
public class FakeJwtEncoder implements JwtEncoder {

    private final List<JwtClaimsSet> encodedClaims = new ArrayList<>();

    @Override
    public Jwt encode(JwtEncoderParameters parameters) {
        var claims = parameters.getClaims();
        encodedClaims.add(claims);
        var builder = Jwt.withTokenValue("test-token-" + encodedClaims.size())
                .header("alg", "none")
                .subject(claims.getSubject());
        var issuer = claims.getClaims().get("iss");
        return (issuer == null ? builder : builder.issuer(issuer.toString())).build();
    }

    public List<JwtClaimsSet> getEncodedClaims() {
        return List.copyOf(encodedClaims);
    }
}

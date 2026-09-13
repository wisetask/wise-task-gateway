package ru.leti.wise.task.gateway.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

@ConfigurationProperties("security.jwt")
public record JwtProperties(
        RSAPublicKey publicKey,
        RSAPrivateKey privateKey,
        Duration accessExpiresAt,
        Duration refreshExpiresAt
) {
}
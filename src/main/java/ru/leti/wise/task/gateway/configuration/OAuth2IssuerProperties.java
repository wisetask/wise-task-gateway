package ru.leti.wise.task.gateway.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.oauth2.trusted-issuers")
public record OAuth2IssuerProperties(
    String googleIssuer,
    String vkIssuer
) {
}

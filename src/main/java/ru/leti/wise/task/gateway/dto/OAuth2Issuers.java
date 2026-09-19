package ru.leti.wise.task.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OAuth2Issuers {
    GOOGLE("https://accounts.google.com");

    final String iss;
}

package ru.leti.wise.task.gateway.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IssuerException extends RuntimeException {
    private final String issuer;
}

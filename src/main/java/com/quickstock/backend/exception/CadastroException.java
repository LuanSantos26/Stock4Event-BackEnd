package com.quickstock.backend.exception;

import org.springframework.http.HttpStatus;

public class CadastroException extends RuntimeException {

    private final HttpStatus status;

    public CadastroException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

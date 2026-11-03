package com.example.app.common.exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }

    public UnauthorizedException() {
        this("Authentication required");
    }

}
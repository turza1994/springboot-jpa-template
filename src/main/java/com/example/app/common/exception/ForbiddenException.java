package com.example.app.common.exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }

    public ForbiddenException() {
        this("Access denied");
    }

}
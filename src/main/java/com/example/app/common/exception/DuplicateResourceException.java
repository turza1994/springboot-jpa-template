package com.example.app.common.exception;

import lombok.Getter;

@Getter
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String resourceName, String field, Object value) {
        super("DUPLICATE_RESOURCE", String.format("%s already exists with %s: %s", resourceName, field, value), resourceName, field, value);
    }

}
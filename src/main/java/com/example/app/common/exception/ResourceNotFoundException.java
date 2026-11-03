package com.example.app.common.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found with identifier: %s", resourceName, identifier), resourceName, identifier);
    }

    public ResourceNotFoundException(String resourceName, String field, Object value) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found with %s: %s", resourceName, field, value), resourceName, field, value);
    }

}
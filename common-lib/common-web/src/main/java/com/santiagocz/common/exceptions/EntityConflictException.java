package com.santiagocz.common.exceptions;

public class EntityConflictException extends RuntimeException {
    public EntityConflictException(String message) {
        super(message);
    }
}
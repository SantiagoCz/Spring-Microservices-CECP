package com.santiagocz.auth_service.exceptions;

public class SubRoleNotFoundException extends RuntimeException {
    public SubRoleNotFoundException(String message) {
        super(message);
    }
}

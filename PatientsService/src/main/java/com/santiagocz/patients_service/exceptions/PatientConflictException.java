package com.santiagocz.patients_service.exceptions;

public class PatientConflictException extends RuntimeException {
    public PatientConflictException(String message) {
        super(message);
    }
}

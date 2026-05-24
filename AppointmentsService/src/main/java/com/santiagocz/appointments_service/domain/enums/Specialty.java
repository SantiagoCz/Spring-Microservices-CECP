package com.santiagocz.appointments_service.domain.enums;

public enum Specialty {
    DENTISTRY("Odontología"),
    PSYCHOLOGY("Psicología"),
    PHYSIOTHERAPY("Kinesiología"),
    NUTRITION("Nutrición");

    private final String displayName;

    Specialty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
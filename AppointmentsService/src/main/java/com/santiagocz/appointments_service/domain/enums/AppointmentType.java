package com.santiagocz.appointments_service.domain.enums;

public enum AppointmentType {
    REGULAR("Turno"),
    OVERBOOKED("Sobre-turno");

    private final String displayName;

    AppointmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
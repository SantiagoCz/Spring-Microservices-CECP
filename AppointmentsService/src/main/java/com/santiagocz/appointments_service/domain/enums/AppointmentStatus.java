package com.santiagocz.appointments_service.domain.enums;

public enum AppointmentStatus {
    SCHEDULED("Programado"),
    CONFIRMED("Confirmado"),
    ATTENDED("Atendido"),
    NO_SHOW("Ausente"),
    CANCELED("Cancelado"),
    DELETED("Eliminado");

    private final String displayName;

    AppointmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
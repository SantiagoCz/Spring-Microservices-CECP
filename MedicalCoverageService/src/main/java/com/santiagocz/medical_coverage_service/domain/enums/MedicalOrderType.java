package com.santiagocz.medical_coverage_service.domain.enums;

public enum MedicalOrderType {
    CONSULTA("Consulta"),
    PRACTICA("Práctica");

    private final String displayName;

    MedicalOrderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
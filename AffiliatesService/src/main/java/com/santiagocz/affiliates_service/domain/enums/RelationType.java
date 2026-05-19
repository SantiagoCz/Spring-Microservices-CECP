package com.santiagocz.affiliates_service.domain.enums;

public enum RelationType {
    CHILD("Hijo/a"),
    DOMESTIC_PARTNER("Concubino/a"),
    SPOUSE("Cónyuge"),
    DIVORCED_DEPENDENT("Pareja divorciada con derechos de dependencia");

    private final String displayName;

    RelationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

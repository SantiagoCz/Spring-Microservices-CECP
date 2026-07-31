package com.santiagocz.auth_service.domain.enums;

public enum HierarchyRole {
    SUPER_ADMIN("Full system access"),
    ADMIN("Create and manage users within their delegation"),
    USER("Common employee, access based on subroles");

    private final String description;

    HierarchyRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
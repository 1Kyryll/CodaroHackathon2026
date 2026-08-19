package com.example.hackathoncodaro2026.model.enums;

public enum Role {
    USER("Player"),
    MANAGER("Manager"),
    ADMIN("Admin"),
    COACH("Coach");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

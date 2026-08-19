package com.example.hackathoncodaro2026.model.enums;

public enum ReservationKind {
    STANDARD("Court booking"),
    INDIVIDUAL("Individual"),
    LESSON("Lesson");

    private final String label;

    ReservationKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

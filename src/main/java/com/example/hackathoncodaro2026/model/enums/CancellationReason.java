package com.example.hackathoncodaro2026.model.enums;

public enum CancellationReason {
    CHANGE_OF_PLANS("Change of plans"),
    WEATHER("Weather"),
    BOOKED_BY_MISTAKE("Booked by mistake"),
    SCHEDULING_CONFLICT("Scheduling conflict"),
    OTHER("Other");

    private final String label;

    CancellationReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static CancellationReason fromPosted(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (CancellationReason reason : values()) {
            if (reason.name().equalsIgnoreCase(trimmed) || reason.label.equalsIgnoreCase(trimmed)) {
                return reason;
            }
        }
        return null;
    }
}

package com.example.hackathoncodaro2026.model.enums;

import java.util.List;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED;

    public static List<ReservationStatus> occupying() {
        return List.of(PENDING, CONFIRMED);
    }
}

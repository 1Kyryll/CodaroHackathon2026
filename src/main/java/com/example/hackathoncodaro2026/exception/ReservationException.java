package com.example.hackathoncodaro2026.exception;

public class ReservationException extends RuntimeException {

    private final String field;

    public ReservationException(String message) {
        super(message);
        this.field = null;
    }

    public ReservationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}

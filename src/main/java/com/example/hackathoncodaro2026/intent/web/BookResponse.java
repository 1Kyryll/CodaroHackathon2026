package com.example.hackathoncodaro2026.intent.web;

/** {@code POST /api/intent/book} response body. */
public record BookResponse(Long reservationId, String status, String totalAmount, String message) {
}

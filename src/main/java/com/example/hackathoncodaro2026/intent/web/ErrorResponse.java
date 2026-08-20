package com.example.hackathoncodaro2026.intent.web;

/** Uniform error body for the {@code /api/**} surface — never a stack trace. */
public record ErrorResponse(String error) {
}

package com.example.hackathoncodaro2026.intent.web;

import jakarta.validation.constraints.NotBlank;

/** {@code POST /api/auth/token} request body. */
public record AuthTokenRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}

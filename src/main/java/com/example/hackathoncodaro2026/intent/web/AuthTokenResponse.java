package com.example.hackathoncodaro2026.intent.web;

import java.time.LocalDateTime;

/** {@code POST /api/auth/token} response body. */
public record AuthTokenResponse(String token, LocalDateTime expiresAt, String displayName) {
}

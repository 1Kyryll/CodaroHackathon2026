package com.example.hackathoncodaro2026.intent.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** {@code POST /api/intent/suggest} request body. */
public record SuggestRequest(
        @NotBlank String text,
        @Min(1) Integer partySize
) {
}

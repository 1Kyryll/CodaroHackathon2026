package com.example.hackathoncodaro2026.intent.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JSON view of {@link com.example.hackathoncodaro2026.intent.model.Suggestion}.
 * {@code price} is omitted (not nulled-out-but-present) when pricing was
 * awkward for this suggestion, per the contract — never an invented number.
 */
public record SuggestionDto(
        long resourceId,
        String resourceName,
        String facilityName,
        LocalDateTime start,
        LocalDateTime end,
        double score,
        String reason,
        @JsonInclude(JsonInclude.Include.NON_NULL) String price,
        List<ScoreTermDto> terms,
        List<String> relaxed
) {
}

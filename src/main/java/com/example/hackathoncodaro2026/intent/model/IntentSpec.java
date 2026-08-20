package com.example.hackathoncodaro2026.intent.model;

import java.time.LocalDate;
import java.util.List;

/**
 * The parsed form of a natural-language booking request. Produced by
 * {@code intent.parse}, consumed by {@code intent.engine}. Contains no
 * domain vocabulary of its own: constraint keys are looked up in
 * {@link com.example.hackathoncodaro2026.intent.config.IntentProperties}.
 */
public record IntentSpec(
        int durationMin,
        LocalDate dayFrom,
        LocalDate dayTo,
        TimeOfDay timeOfDay,
        List<String> hardConstraints,
        List<String> softConstraints,
        String resourceType,
        int partySize
) {
    public IntentSpec {
        hardConstraints = hardConstraints == null ? List.of() : List.copyOf(hardConstraints);
        softConstraints = softConstraints == null ? List.of() : List.copyOf(softConstraints);
        timeOfDay = timeOfDay == null ? TimeOfDay.ANY : timeOfDay;
        partySize = Math.max(1, partySize);
    }
}

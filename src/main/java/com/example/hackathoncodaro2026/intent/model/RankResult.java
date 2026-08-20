package com.example.hackathoncodaro2026.intent.model;

import java.util.List;

/**
 * The engine's output. An empty {@code suggestions} list MUST come with a
 * non-empty {@code relaxationTrail} explaining what was tried.
 */
public record RankResult(List<Suggestion> suggestions, List<RelaxStep> relaxationTrail) {
    public RankResult {
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        relaxationTrail = relaxationTrail == null ? List.of() : List.copyOf(relaxationTrail);
    }

    public static RankResult empty() {
        return new RankResult(List.of(), List.of());
    }
}

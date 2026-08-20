package com.example.hackathoncodaro2026.intent.web;

/** JSON view of {@link com.example.hackathoncodaro2026.intent.model.ScoreTerm}. */
public record ScoreTermDto(String key, String label, double delta, boolean satisfied) {
}

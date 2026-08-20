package com.example.hackathoncodaro2026.intent.model;

/**
 * One weighted contribution to a suggestion's score. {@code label} is
 * human-readable and comes from config, never from a literal in the engine.
 */
public record ScoreTerm(String key, String label, double delta, boolean satisfied) {
}

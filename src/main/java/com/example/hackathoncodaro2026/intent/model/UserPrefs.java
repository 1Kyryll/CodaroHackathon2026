package com.example.hackathoncodaro2026.intent.model;

/**
 * Per-user scheduling preferences. {@code preferredFromMin}/{@code preferredToMin}
 * are minutes since midnight; a zero-width window means "no stated preference".
 */
public record UserPrefs(int preferredFromMin, int preferredToMin, int minBufferMin) {

    public static UserPrefs none() {
        return new UserPrefs(0, 0, 0);
    }

    public boolean hasPreferredWindow() {
        return preferredToMin > preferredFromMin;
    }
}

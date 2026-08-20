package com.example.hackathoncodaro2026.intent.engine;

import java.time.LocalDateTime;

/**
 * A raw generated slot before filtering/scoring, plus the free interval it
 * was carved out of (needed by the buffer and fragmentation score terms).
 * Not part of the public contract — never crosses the {@code SlotRanker}
 * boundary.
 */
record Candidate(long resourceId, LocalDateTime start, LocalDateTime end, Interval sourceInterval) {
}

package com.example.hackathoncodaro2026.intent.engine;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * A half-open time span {@code [start, end)}. Pure value type used only
 * inside the engine — never crosses the {@code SlotRanker} boundary.
 */
record Interval(LocalDateTime start, LocalDateTime end) implements Comparable<Interval> {

    Interval {
        if (end.isBefore(start)) {
            end = start;
        }
    }

    long minutes() {
        return Duration.between(start, end).toMinutes();
    }

    boolean isEmpty() {
        return !end.isAfter(start);
    }

    boolean overlaps(Interval other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    @Override
    public int compareTo(Interval o) {
        int byStart = start.compareTo(o.start);
        return byStart != 0 ? byStart : end.compareTo(o.end);
    }
}

package com.example.hackathoncodaro2026.intent.engine;

import com.example.hackathoncodaro2026.intent.config.IntentProperties;
import com.example.hackathoncodaro2026.intent.model.ResourceSlice;
import com.example.hackathoncodaro2026.intent.model.ScheduleSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression: capacity and party size are different things.
 *
 * {@code capacity} counts how many occupancy units may share the resource at
 * the same time — a court is 1, a gym floor may be 18. {@code maxPartySize} is
 * how many people one booking may contain. Filtering party size against
 * capacity rejected every court booking for more than one person, which made
 * the whole feature return zero suggestions for "tennis for two".
 */
class PartySizeCapacityTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 9, 0);

    /** A court: one booking at a time, but two to four people in it. */
    private ResourceSlice court() {
        return new ResourceSlice(1L, "Tennis Court 1", "Site", "TYPE1",
                1, 2, 4,
                LocalTime.of(8, 0), LocalTime.of(22, 0), 60, Map.of());
    }

    private IntentProperties config() {
        return new IntentProperties(15, 10, 3, 12, 0.25,
                new IntentProperties.Weights(20, 15, 15, 20, 15, 15), List.of(), Map.of());
    }

    private ScheduleSnapshot snapshot() {
        return new ScheduleSnapshot(List.of(), List.of(), NOW, 42L, null);
    }

    private Candidate candidate() {
        LocalDateTime start = NOW.plusHours(2);
        return new Candidate(1L, start, start.plusMinutes(60),
                new Interval(NOW.plusHours(1), NOW.plusHours(8)));
    }

    @Test
    void admitsPartyLargerThanCapacityWhenWithinPartySizeRange() {
        assertTrue(ConstraintFilter.admits(candidate(), court(), snapshot(), config(), List.of(), 2),
                "a capacity-1 court must still accept a 2-person booking");
        assertTrue(ConstraintFilter.admits(candidate(), court(), snapshot(), config(), List.of(), 4),
                "party size up to maxPartySize must be admitted");
    }

    @Test
    void rejectsPartyLargerThanMaxPartySize() {
        assertFalse(ConstraintFilter.admits(candidate(), court(), snapshot(), config(), List.of(), 5),
                "party size above maxPartySize must be rejected");
    }

    @Test
    void admitsPartySmallerThanMinimum() {
        // Not a scheduling conflict: the booking is simply made at the minimum.
        // Rejecting here would make an unspecified party size return nothing.
        assertTrue(ConstraintFilter.admits(candidate(), court(), snapshot(), config(), List.of(), 1),
                "an unstated/small party must not eliminate every court");
    }
}

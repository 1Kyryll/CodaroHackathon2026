package com.example.hackathoncodaro2026.intent.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the engine is allowed to know, loaded once by the service layer
 * before ranking runs. This is the I/O boundary: nothing downstream of here
 * touches a repository.
 *
 * {@code now} is passed in rather than read from the clock so that ranking is
 * deterministic and testable.
 */
public record ScheduleSnapshot(
        List<ResourceSlice> resources,
        List<ReservationSlice> reservations,
        LocalDateTime now,
        long requestingUserId,
        UserPrefs prefs
) {
    public ScheduleSnapshot {
        resources = resources == null ? List.of() : List.copyOf(resources);
        reservations = reservations == null ? List.of() : List.copyOf(reservations);
        prefs = prefs == null ? UserPrefs.none() : prefs;
    }

    public List<ReservationSlice> reservationsFor(long resourceId) {
        return reservations.stream().filter(r -> r.resourceId() == resourceId).toList();
    }
}

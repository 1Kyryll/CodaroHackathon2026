package com.example.hackathoncodaro2026.intent.model;

import java.time.LocalTime;
import java.util.Map;

/**
 * A bookable resource, flattened to plain data. Deliberately NOT the
 * {@code SportResource} entity: the engine must stay free of JPA so it can be
 * unit-tested without a database and without lazy-loading hazards.
 *
 * {@code capacity} is how many occupancy units may share the resource at once
 * (a court is 1, a gym floor may be 18). It is NOT a headcount limit for a
 * single booking — that is {@code minPartySize}/{@code maxPartySize}. Conflating
 * the two rejects every court booking for more than one person.
 *
 * {@code attributes} carries domain-specific flags (indoor, lit, coach-available…)
 * that config constraints test by key. The engine never reads a specific key.
 */
public record ResourceSlice(
        long id,
        String name,
        String facilityName,
        String typeKey,
        int capacity,
        int minPartySize,
        int maxPartySize,
        LocalTime opening,
        LocalTime closing,
        int slotDurationMinutes,
        Map<String, Object> attributes
) {
    public ResourceSlice {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        minPartySize = Math.max(1, minPartySize);
        maxPartySize = Math.max(minPartySize, maxPartySize);
    }

    /**
     * Convenience for tests and callers that do not model party size: one
     * person minimum, {@code capacity} maximum.
     */
    public ResourceSlice(
            long id,
            String name,
            String facilityName,
            String typeKey,
            int capacity,
            LocalTime opening,
            LocalTime closing,
            int slotDurationMinutes,
            Map<String, Object> attributes
    ) {
        this(id, name, facilityName, typeKey, capacity, 1, Math.max(1, capacity),
                opening, closing, slotDurationMinutes, attributes);
    }

    public Object attribute(String key) {
        return attributes.get(key);
    }
}

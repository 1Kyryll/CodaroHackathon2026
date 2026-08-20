package com.example.hackathoncodaro2026.intent.model;

import java.time.LocalTime;
import java.util.Map;

/**
 * A bookable resource, flattened to plain data. Deliberately NOT the
 * {@code SportResource} entity: the engine must stay free of JPA so it can be
 * unit-tested without a database and without lazy-loading hazards.
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
        LocalTime opening,
        LocalTime closing,
        int slotDurationMinutes,
        Map<String, Object> attributes
) {
    public ResourceSlice {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public Object attribute(String key) {
        return attributes.get(key);
    }
}

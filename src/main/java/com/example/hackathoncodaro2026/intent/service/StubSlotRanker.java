package com.example.hackathoncodaro2026.intent.service;

import com.example.hackathoncodaro2026.intent.config.IntentProperties;
import com.example.hackathoncodaro2026.intent.engine.SlotRanker;
import com.example.hackathoncodaro2026.intent.model.IntentSpec;
import com.example.hackathoncodaro2026.intent.model.RankResult;
import com.example.hackathoncodaro2026.intent.model.RelaxStep;
import com.example.hackathoncodaro2026.intent.model.ReservationSlice;
import com.example.hackathoncodaro2026.intent.model.ResourceSlice;
import com.example.hackathoncodaro2026.intent.model.ScheduleSnapshot;
import com.example.hackathoncodaro2026.intent.model.ScoreTerm;
import com.example.hackathoncodaro2026.intent.model.Suggestion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TEMPORARY — replaced by {@code com.example.hackathoncodaro2026.intent.engine.DefaultSlotRanker}
 * once that class lands (it is being written in parallel). Swapping is a
 * one-line change in {@link IntentBeansConfig#slotRanker()}.
 *
 * A minimal, honest "first free slot" ranker: filters by resource type and
 * capacity, walks the requested day at the config granularity looking for a
 * gap of the requested duration, and returns the earliest one per resource.
 * It never lies about why a slot was picked — the reason always says
 * "stub ranker" so nobody mistakes this for the real scoring engine.
 */
public final class StubSlotRanker implements SlotRanker {

    @Override
    public RankResult rank(IntentSpec spec, ScheduleSnapshot snapshot, IntentProperties config) {
        int duration = spec.durationMin() <= 0 ? 60 : spec.durationMin();
        LocalDate day = spec.dayFrom() == null ? snapshot.now().toLocalDate() : spec.dayFrom();
        int granularity = config.granularityMin() <= 0 ? 15 : config.granularityMin();
        int maxSuggestions = config.maxSuggestions() <= 0 ? 3 : config.maxSuggestions();

        List<Suggestion> suggestions = new ArrayList<>();
        for (ResourceSlice resource : snapshot.resources()) {
            if (suggestions.size() >= maxSuggestions) {
                break;
            }
            if (spec.resourceType() != null && !spec.resourceType().isBlank()
                    && !spec.resourceType().equalsIgnoreCase(resource.typeKey())) {
                continue;
            }
            if (resource.capacity() < spec.partySize()) {
                continue;
            }
            findFirstFreeSlot(resource, day, duration, granularity, snapshot).ifPresent(suggestions::add);
        }

        if (suggestions.isEmpty()) {
            return new RankResult(List.of(), List.of(new RelaxStep(
                    RelaxStep.Action.WIDEN_DAY_WINDOW,
                    "Stub ranker (temporary) found no free slot matching the request on " + day + ".",
                    List.of()
            )));
        }
        return new RankResult(suggestions, List.of());
    }

    private java.util.Optional<Suggestion> findFirstFreeSlot(
            ResourceSlice resource,
            LocalDate day,
            int durationMin,
            int granularityMin,
            ScheduleSnapshot snapshot
    ) {
        LocalDateTime cursor = LocalDateTime.of(day, resource.opening());
        LocalDateTime dayClose = LocalDateTime.of(day, resource.closing());
        List<ReservationSlice> occupying = snapshot.reservationsFor(resource.id());
        while (!cursor.plusMinutes(durationMin).isAfter(dayClose)) {
            LocalDateTime candidateStart = cursor;
            LocalDateTime candidateEnd = cursor.plusMinutes(durationMin);
            boolean free = candidateStart.isAfter(snapshot.now())
                    && occupying.stream().noneMatch(r -> r.start().isBefore(candidateEnd) && r.end().isAfter(candidateStart));
            if (free) {
                return java.util.Optional.of(new Suggestion(
                        resource.id(),
                        resource.name(),
                        resource.facilityName(),
                        candidateStart,
                        candidateEnd,
                        50.0,
                        List.of(new ScoreTerm("availability", "first available slot", 50.0, true)),
                        "Earliest open slot at " + resource.name() + " (stub ranker — temporary, not yet the scored engine).",
                        List.of()
                ));
            }
            cursor = cursor.plusMinutes(granularityMin);
        }
        return java.util.Optional.empty();
    }
}

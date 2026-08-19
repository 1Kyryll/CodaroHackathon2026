package com.example.hackathoncodaro2026.service;

import com.example.hackathoncodaro2026.dto.TimeSlotView;
import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.enums.ReservationKind;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ResourceService {

    List<SportResource> findEnabledByFacility(Long facilityId);

    Optional<SportResource> findEnabledWithFacility(Long id);

    List<TimeSlotView> slotsFor(SportResource resource, LocalDate date);

    List<TimeSlotView> slotsFor(SportResource resource, LocalDate date, ReservationKind kind);

    long countEnabled();
}

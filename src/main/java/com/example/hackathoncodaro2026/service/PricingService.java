package com.example.hackathoncodaro2026.service;

import com.example.hackathoncodaro2026.model.InventoryItem;
import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.enums.ReservationKind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;

public interface PricingService {

    BigDecimal quote(SportResource resource, LocalDate date, LocalTime start, int durationHours);

    BigDecimal quote(
            SportResource resource,
            LocalDate date,
            LocalTime start,
            int durationHours,
            ReservationKind kind,
            Collection<InventoryItem> extras,
            int people
    );

    BigDecimal quote(
            SportResource resource,
            LocalDate date,
            LocalTime start,
            int durationHours,
            ReservationKind kind,
            Collection<InventoryItem> extras,
            int people,
            BigDecimal coachFee
    );

    BigDecimal hourlyRate(SportResource resource, LocalDate date, LocalTime hourStart);

    BigDecimal hourlyRate(SportResource resource, LocalDate date, LocalTime hourStart, ReservationKind kind);
}

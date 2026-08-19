package com.example.hackathoncodaro2026.service.impl;

import com.example.hackathoncodaro2026.model.InventoryItem;
import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.enums.ReservationKind;
import com.example.hackathoncodaro2026.service.PricingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Service
public class PricingServiceImpl implements PricingService {

    private static final BigDecimal EVENING_MULTIPLIER = new BigDecimal("1.35");
    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.25");
    private static final LocalTime EVENING_START = LocalTime.of(17, 0);
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Override
    public BigDecimal quote(SportResource resource, LocalDate date, LocalTime start, int durationHours) {
        return quote(resource, date, start, durationHours, ReservationKind.STANDARD, List.of(), 1);
    }

    @Override
    public BigDecimal quote(
            SportResource resource,
            LocalDate date,
            LocalTime start,
            int durationHours,
            ReservationKind kind,
            Collection<InventoryItem> extras,
            int people
    ) {
        return quote(resource, date, start, durationHours, kind, extras, people, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal quote(
            SportResource resource,
            LocalDate date,
            LocalTime start,
            int durationHours,
            ReservationKind kind,
            Collection<InventoryItem> extras,
            int people,
            BigDecimal coachFee
    ) {
        if (resource == null || date == null || start == null || durationHours < 1) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        BigDecimal total = BigDecimal.ZERO;
        LocalTime cursor = start;
        for (int hour = 0; hour < durationHours; hour++) {
            total = total.add(hourlyRate(resource, date, cursor, kind));
            cursor = cursor.plusHours(1);
        }
        int heads = people < 1 ? 1 : people;
        if (extras != null) {
            for (InventoryItem extra : extras) {
                if (extra == null || extra.getPricePerPerson() == null) {
                    continue;
                }
                total = total.add(extra.getPricePerPerson().multiply(BigDecimal.valueOf(heads)));
            }
        }
        if (coachFee != null && coachFee.signum() > 0) {
            total = total.add(coachFee);
        }
        return total.setScale(SCALE, ROUNDING);
    }

    @Override
    public BigDecimal hourlyRate(SportResource resource, LocalDate date, LocalTime hourStart) {
        return hourlyRate(resource, date, hourStart, ReservationKind.STANDARD);
    }

    @Override
    public BigDecimal hourlyRate(SportResource resource, LocalDate date, LocalTime hourStart, ReservationKind kind) {
        BigDecimal rate = kind == ReservationKind.LESSON
                ? resource.effectiveLessonHourlyPrice()
                : resource.effectiveBaseHourlyPrice();
        if (!hourStart.isBefore(EVENING_START)) {
            rate = rate.multiply(EVENING_MULTIPLIER);
        }
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            rate = rate.multiply(WEEKEND_MULTIPLIER);
        }
        return rate.setScale(SCALE, ROUNDING);
    }
}

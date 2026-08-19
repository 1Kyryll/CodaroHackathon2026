package com.example.hackathoncodaro2026.service.impl;

import com.example.hackathoncodaro2026.dto.TimeSlotView;
import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.enums.ReservationKind;
import com.example.hackathoncodaro2026.model.enums.ReservationStatus;
import com.example.hackathoncodaro2026.repository.ReservationRepository;
import com.example.hackathoncodaro2026.repository.SportResourceRepository;
import com.example.hackathoncodaro2026.service.ResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {

    static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final SportResourceRepository sportResourceRepository;
    private final ReservationRepository reservationRepository;

    public ResourceServiceImpl(
            SportResourceRepository sportResourceRepository,
            ReservationRepository reservationRepository
    ) {
        this.sportResourceRepository = sportResourceRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public List<SportResource> findEnabledByFacility(Long facilityId) {
        return sportResourceRepository.findByFacility_IdAndEnabledTrueOrderByNameAsc(facilityId);
    }

    @Override
    public Optional<SportResource> findEnabledWithFacility(Long id) {
        return sportResourceRepository.findWithFacilityById(id)
                .filter(resource -> resource.isEnabled() && resource.getFacility().isEnabled());
    }

    @Override
    public List<TimeSlotView> slotsFor(SportResource resource, LocalDate date) {
        return slotsFor(resource, date, ReservationKind.STANDARD);
    }

    @Override
    public List<TimeSlotView> slotsFor(SportResource resource, LocalDate date, ReservationKind kind) {
        List<TimeSlotView> slots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(WARSAW);
        LocalTime cursor = resource.getOpeningTime();
        int duration = resource.getSlotDurationMinutes();
        boolean lesson = kind == ReservationKind.LESSON;
        while (!cursor.plusMinutes(duration).isAfter(resource.getClosingTime())) {
            LocalTime start = cursor;
            LocalTime end = cursor.plusMinutes(duration);
            LocalDateTime startAt = LocalDateTime.of(date, start);
            LocalDateTime endAt = LocalDateTime.of(date, end);
            long booked = reservationRepository.countOverlapping(
                    resource.getId(),
                    ReservationStatus.occupying(),
                    startAt,
                    endAt
            );
            boolean remaining = lesson ? booked == 0 : booked < resource.getCapacity();
            boolean future = startAt.isAfter(now);
            slots.add(new TimeSlotView(start, end, (int) booked, resource.getCapacity(), remaining && future));
            cursor = end;
        }
        return slots;
    }

    @Override
    public long countEnabled() {
        return sportResourceRepository.countByEnabledTrue();
    }
}

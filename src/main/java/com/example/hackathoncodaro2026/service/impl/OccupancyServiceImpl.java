package com.example.hackathoncodaro2026.service.impl;

import com.example.hackathoncodaro2026.dto.OccupancyCell;
import com.example.hackathoncodaro2026.dto.OccupancyGrid;
import com.example.hackathoncodaro2026.dto.OccupancyRow;
import com.example.hackathoncodaro2026.model.Reservation;
import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.enums.ReservationStatus;
import com.example.hackathoncodaro2026.repository.ReservationRepository;
import com.example.hackathoncodaro2026.repository.SportResourceRepository;
import com.example.hackathoncodaro2026.service.OccupancyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OccupancyServiceImpl implements OccupancyService {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final SportResourceRepository sportResourceRepository;
    private final ReservationRepository reservationRepository;

    public OccupancyServiceImpl(
            SportResourceRepository sportResourceRepository,
            ReservationRepository reservationRepository
    ) {
        this.sportResourceRepository = sportResourceRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public OccupancyGrid gridFor(LocalDate date, Long facilityId) {
        List<SportResource> resources = facilityId == null
                ? sportResourceRepository.findAllEnabledWithFacility()
                : sportResourceRepository.findEnabledWithFacilityByFacilityId(facilityId);
        OccupancyGrid grid = new OccupancyGrid();
        grid.setDate(date);
        grid.setFacilityId(facilityId);
        if (resources.isEmpty()) {
            return grid;
        }
        LocalTime open = resources.stream().map(SportResource::getOpeningTime).min(LocalTime::compareTo).orElse(LocalTime.of(7, 0));
        LocalTime close = resources.stream().map(SportResource::getClosingTime).max(LocalTime::compareTo).orElse(LocalTime.of(22, 0));
        List<LocalTime> hours = new ArrayList<>();
        LocalTime cursor = open;
        while (cursor.isBefore(close)) {
            hours.add(cursor);
            cursor = cursor.plusHours(1);
        }
        grid.setHours(hours);
        LocalDateTime dayStart = date.atTime(open);
        LocalDateTime dayEnd = date.atTime(close);
        List<Reservation> reservations = reservationRepository.findOccupyingOverlapping(
                ReservationStatus.occupying(),
                dayStart,
                dayEnd
        );
        Map<Long, List<Reservation>> byResource = reservations.stream()
                .collect(Collectors.groupingBy(item -> item.getResource().getId()));
        LocalDateTime now = LocalDateTime.now(WARSAW);
        int bookedUnits = 0;
        int capacityUnits = 0;
        List<OccupancyRow> rows = new ArrayList<>();
        for (SportResource resource : resources) {
            OccupancyRow row = new OccupancyRow();
            row.setResourceId(resource.getId());
            row.setResourceName(resource.getName());
            row.setFacilityName(resource.getFacility().getName());
            row.setSport(resource.getType().getDisplayName());
            row.setImagePath(resource.getImagePath() != null ? resource.getImagePath() : resource.getType().getImagePath());
            row.setCapacity(resource.getCapacity());
            List<Reservation> booked = byResource.getOrDefault(resource.getId(), List.of());
            List<OccupancyCell> cells = new ArrayList<>();
            for (LocalTime hour : hours) {
                LocalDateTime slotStart = date.atTime(hour);
                LocalDateTime slotEnd = slotStart.plusHours(1);
                boolean openSlot = !hour.isBefore(resource.getOpeningTime())
                        && !slotEnd.toLocalTime().isAfter(resource.getClosingTime())
                        && !slotEnd.toLocalDate().isAfter(date);
                if (!openSlot) {
                    cells.add(new OccupancyCell(hour, 0, resource.getCapacity(), "closed", false));
                    continue;
                }
                int count = 0;
                for (Reservation reservation : booked) {
                    if (reservation.getStartAt().isBefore(slotEnd) && reservation.getEndAt().isAfter(slotStart)) {
                        count += Math.max(1, reservation.getOccupancyUnits());
                    }
                }
                boolean past = !slotStart.isAfter(now);
                boolean full = count >= resource.getCapacity();
                String level = levelFor(count, resource.getCapacity(), past);
                boolean bookable = !full && !past;
                cells.add(new OccupancyCell(hour, count, resource.getCapacity(), level, bookable));
                bookedUnits += count;
                capacityUnits += resource.getCapacity();
            }
            row.setCells(cells);
            rows.add(row);
        }
        grid.setRows(rows);
        grid.setBookedUnits(bookedUnits);
        grid.setCapacityUnits(capacityUnits);
        return grid;
    }

    private String levelFor(int booked, int capacity, boolean past) {
        if (booked >= capacity) {
            return "full";
        }
        if (past) {
            return booked == 0 ? "past" : "past-used";
        }
        if (booked == 0) {
            return "free";
        }
        double ratio = booked / (double) capacity;
        if (ratio < 0.5) {
            return "low";
        }
        if (ratio < 0.85) {
            return "partial";
        }
        return "near";
    }
}

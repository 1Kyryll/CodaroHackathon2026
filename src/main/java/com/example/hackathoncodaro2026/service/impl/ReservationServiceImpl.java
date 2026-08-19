package com.example.hackathoncodaro2026.service.impl;

import com.example.hackathoncodaro2026.dto.ReservationRequest;
import com.example.hackathoncodaro2026.exception.ReservationException;
import com.example.hackathoncodaro2026.model.CoachOffering;
import com.example.hackathoncodaro2026.model.InventoryItem;
import com.example.hackathoncodaro2026.model.Reservation;
import com.example.hackathoncodaro2026.model.ReservationExtra;
import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.CancellationReason;
import com.example.hackathoncodaro2026.model.enums.ReservationKind;
import com.example.hackathoncodaro2026.model.enums.ReservationStatus;
import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.repository.CoachRatingRepository;
import com.example.hackathoncodaro2026.repository.InventoryItemRepository;
import com.example.hackathoncodaro2026.repository.ReservationExtraRepository;
import com.example.hackathoncodaro2026.repository.ReservationRepository;
import com.example.hackathoncodaro2026.repository.SportResourceRepository;
import com.example.hackathoncodaro2026.repository.UserRepository;
import com.example.hackathoncodaro2026.service.CoachOfferingService;
import com.example.hackathoncodaro2026.service.PricingService;
import com.example.hackathoncodaro2026.service.ReservationService;
import com.example.hackathoncodaro2026.service.SportSkillLevelCatalog;
import com.example.hackathoncodaro2026.service.UserService;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final Pattern PHONE = Pattern.compile("^[+]?[0-9\\s().-]{7,20}$");

    private final ReservationRepository reservationRepository;
    private final SportResourceRepository sportResourceRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PricingService pricingService;
    private final InventoryItemRepository inventoryItemRepository;
    private final ReservationExtraRepository reservationExtraRepository;
    private final CoachRatingRepository coachRatingRepository;
    private final CoachOfferingService coachOfferingService;
    private final SportSkillLevelCatalog sportSkillLevelCatalog;

    public ReservationServiceImpl(
            ReservationRepository reservationRepository,
            SportResourceRepository sportResourceRepository,
            UserRepository userRepository,
            UserService userService,
            PricingService pricingService,
            InventoryItemRepository inventoryItemRepository,
            ReservationExtraRepository reservationExtraRepository,
            CoachRatingRepository coachRatingRepository,
            CoachOfferingService coachOfferingService,
            SportSkillLevelCatalog sportSkillLevelCatalog
    ) {
        this.reservationRepository = reservationRepository;
        this.sportResourceRepository = sportResourceRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.pricingService = pricingService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.reservationExtraRepository = reservationExtraRepository;
        this.coachRatingRepository = coachRatingRepository;
        this.coachOfferingService = coachOfferingService;
        this.sportSkillLevelCatalog = sportSkillLevelCatalog;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reservation create(User user, ReservationRequest request) {
        SportResource resource;
        try {
            resource = sportResourceRepository.lockById(request.getResourceId())
                    .orElseThrow(() -> new ReservationException("resourceId", "That court could not be found"));
        } catch (TransientDataAccessException ex) {
            throw new ReservationException("This slot was just booked, pick another");
        }
        if (!resource.isEnabled() || resource.getFacility() == null || !resource.getFacility().isEnabled()) {
            throw new ReservationException("resourceId", "This venue is not open for booking");
        }
        if (request.getPaymentMethod() == null) {
            throw new ReservationException("paymentMethod", "Choose a payment method");
        }
        ReservationKind kind = resolveKind(resource, request);
        int partySize = resolvePartySize(resource, request, kind);
        List<InventoryItem> extras = resolveExtras(resource, request.getExtraIds());
        LocalDateTime startAt = LocalDateTime.of(request.getDate(), request.getStartTime());
        int hours = request.getDurationHours() == null ? 1 : request.getDurationHours();
        if (hours < 1 || hours > 4) {
            throw new ReservationException("durationHours", "Duration must be between 1 and 4 hours");
        }
        int durationMinutes = hours * 60;
        if (durationMinutes % resource.getSlotDurationMinutes() != 0) {
            throw new ReservationException(
                    "durationHours",
                    "Duration must be a multiple of " + resource.getSlotDurationMinutes() + " minutes"
            );
        }
        LocalDateTime endAt = startAt.plusMinutes(durationMinutes);
        LocalDateTime now = LocalDateTime.now(WARSAW);
        if (!startAt.isAfter(now)) {
            throw new ReservationException("startTime", "You cannot book a slot in the past");
        }
        if (!endAt.isAfter(startAt)) {
            throw new ReservationException("startTime", "End time must be after start time");
        }
        if (!isAligned(resource, request.getStartTime())) {
            throw new ReservationException(
                    "startTime",
                    "Start time must match a " + resource.getSlotDurationMinutes() + "-minute slot"
            );
        }
        if (request.getStartTime().isBefore(resource.getOpeningTime())
                || endAt.toLocalTime().isAfter(resource.getClosingTime())
                || endAt.toLocalDate().isAfter(request.getDate())) {
            throw new ReservationException("durationHours", "That duration sits outside opening hours");
        }
        User occupant = applyBookingPhone(user, request);
        AssignedCoach assignedCoach = resolveCoach(resource, kind, request, hours);
        LocalTime cursor = request.getStartTime();
        while (cursor.isBefore(endAt.toLocalTime())) {
            LocalDateTime slotStart = LocalDateTime.of(request.getDate(), cursor);
            LocalDateTime slotEnd = slotStart.plusMinutes(resource.getSlotDurationMinutes());
            long booked = reservationRepository.countOverlapping(
                    resource.getId(),
                    ReservationStatus.occupying(),
                    slotStart,
                    slotEnd
            );
            if (kind == ReservationKind.LESSON) {
                if (booked > 0) {
                    throw new ReservationException(
                            "startTime",
                            "This lesson is not available because people are already coming"
                    );
                }
            } else if (booked >= resource.getCapacity()) {
                throw new ReservationException("startTime", "That slot is fully booked");
            }
            cursor = cursor.plusMinutes(resource.getSlotDurationMinutes());
        }
        if (assignedCoach != null) {
            long coachBooked = reservationRepository.countCoachOverlapping(
                    assignedCoach.coach().getId(),
                    ReservationStatus.occupying(),
                    startAt,
                    endAt
            );
            if (coachBooked > 0) {
                throw new ReservationException("coachId", "That coach is no longer available for this time");
            }
        }
        BigDecimal coachFee = assignedCoach == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : assignedCoach.fee();
        Reservation reservation = new Reservation();
        reservation.setUser(occupant);
        reservation.setResource(resource);
        reservation.setStartAt(startAt);
        reservation.setEndAt(endAt);
        reservation.setPartySize(partySize);
        reservation.setPaymentMethod(request.getPaymentMethod());
        reservation.setKind(kind);
        reservation.setOccupancyUnits(kind == ReservationKind.LESSON ? resource.getCapacity() : 1);
        if (assignedCoach != null) {
            reservation.setCoach(assignedCoach.coach());
            reservation.setSkillLevel(assignedCoach.level());
        } else if (request.getSkillLevel() != null && !request.getSkillLevel().isBlank()
                && sportSkillLevelCatalog.isValid(resource.getType(), request.getSkillLevel().trim())) {
            reservation.setSkillLevel(request.getSkillLevel().trim());
        }
        reservation.setTotalAmount(pricingService.quote(
                resource,
                request.getDate(),
                request.getStartTime(),
                hours,
                kind,
                extras,
                partySize,
                coachFee
        ));
        boolean staff = occupant.getRole() == Role.ADMIN || occupant.getRole() == Role.MANAGER;
        reservation.setStatus(staff ? ReservationStatus.CONFIRMED : ReservationStatus.PENDING);
        if (request.getNote() != null && !request.getNote().isBlank()) {
            reservation.setNote(request.getNote().trim());
        }
        for (InventoryItem item : extras) {
            ReservationExtra extra = new ReservationExtra();
            extra.setItem(item);
            extra.setQuantity(partySize);
            reservation.addExtra(extra);
        }
        if (assignedCoach != null) {
            ReservationExtra coachExtra = new ReservationExtra();
            coachExtra.setDescription("Coach " + assignedCoach.coach().getFullName());
            coachExtra.setQuantity(1);
            coachExtra.setUnitAmount(coachFee);
            reservation.addExtra(coachExtra);
        }
        if (reservation.getSkillLevel() != null) {
            userService.saveSportLevel(occupant, resource.getType(), reservation.getSkillLevel());
        }
        try {
            return reservationRepository.save(reservation);
        } catch (TransientDataAccessException ex) {
            throw new ReservationException("This slot was just booked, pick another");
        }
    }

    @Override
    @Transactional
    public void cancel(User actor, Long reservationId) {
        throw new ReservationException("Choose a cancellation reason");
    }

    @Override
    @Transactional
    public void cancel(User actor, Long reservationId, String reason) {
        cancel(actor, reservationId, reason, null);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(User actor, Long reservationId, String reason, String otherNote) {
        Reservation reservation = reservationRepository.findWithDetailsById(reservationId)
                .orElseThrow(() -> new ReservationException("That reservation could not be found"));
        try {
            sportResourceRepository.lockById(reservation.getResource().getId())
                    .orElseThrow(() -> new ReservationException("That court could not be found"));
        } catch (TransientDataAccessException ex) {
            throw new ReservationException("This reservation could not be updated, try again");
        }
        boolean owner = reservation.getUser().getId().equals(actor.getId());
        boolean staff = actor.getRole() == Role.ADMIN || actor.getRole() == Role.MANAGER;
        if (!owner && !staff) {
            throw new ReservationException("You can only cancel your own reservation");
        }
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new ReservationException("Confirmed reservations cannot be cancelled");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ReservationException("Only a pending reservation can be cancelled");
        }
        if (!reservation.getStartAt().isAfter(LocalDateTime.now(WARSAW))) {
            throw new ReservationException("Past reservations cannot be cancelled");
        }
        CancellationReason parsed = CancellationReason.fromPosted(reason);
        if (parsed == null) {
            throw new ReservationException("Choose a cancellation reason");
        }
        String stored = parsed.getLabel();
        if (parsed == CancellationReason.OTHER && otherNote != null && !otherNote.isBlank()) {
            String extra = otherNote.trim();
            if (extra.length() > 400) {
                throw new ReservationException("Cancellation note must be 400 characters or fewer");
            }
            stored = parsed.getLabel() + ": " + extra;
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancellationReason(stored);
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void confirm(User actor, Long reservationId) {
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.MANAGER) {
            throw new ReservationException("Only a manager can confirm a reservation");
        }
        Reservation reservation = reservationRepository.findWithDetailsById(reservationId)
                .orElseThrow(() -> new ReservationException("That reservation could not be found"));
        try {
            sportResourceRepository.lockById(reservation.getResource().getId())
                    .orElseThrow(() -> new ReservationException("That court could not be found"));
        } catch (TransientDataAccessException ex) {
            throw new ReservationException("This reservation could not be updated, try again");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ReservationException("Only a pending reservation can be confirmed");
        }
        if (!reservation.getStartAt().isAfter(LocalDateTime.now(WARSAW))) {
            throw new ReservationException("Past reservations cannot be confirmed");
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
    }

    @Override
    public List<Reservation> findForUser(User user) {
        return reservationRepository.findByUserIdWithDetails(user.getId());
    }

    @Override
    public List<Reservation> findAll() {
        return reservationRepository.findAllWithDetails();
    }

    @Override
    public List<Reservation> findManagerQueue(LocalDate date) {
        LocalDate selected = date == null ? LocalDate.now(WARSAW) : date;
        return reservationRepository.findQueueForDate(
                selected.atStartOfDay(),
                selected.plusDays(1).atStartOfDay(),
                ReservationStatus.occupying()
        );
    }

    @Override
    public List<Reservation> findForCoach(User coach) {
        return reservationRepository.findByCoachIdWithDetails(coach.getId());
    }

    @Override
    public long countUpcomingActive(User user) {
        return reservationRepository.countByUserAndStatusInAndStartAtAfter(
                user,
                ReservationStatus.occupying(),
                LocalDateTime.now(WARSAW)
        );
    }

    @Override
    public long countUpcomingAsCoach(User coach) {
        return reservationRepository.countByCoachAndStatusInAndStartAtAfter(
                coach,
                ReservationStatus.occupying(),
                LocalDateTime.now(WARSAW)
        );
    }

    @Override
    @Transactional
    public int deleteEndedBefore(LocalDateTime cutoff) {
        coachRatingRepository.deleteForReservationsEndedBefore(cutoff);
        reservationExtraRepository.deleteForReservationsEndedBefore(cutoff);
        return reservationRepository.deleteEndedBefore(cutoff);
    }

    @Override
    @Transactional
    public int deleteEndedOlderThanOneMonth() {
        return deleteEndedBefore(LocalDateTime.now(WARSAW).minusMonths(1));
    }

    private ReservationKind resolveKind(SportResource resource, ReservationRequest request) {
        if (!resource.requiresBookingMode()) {
            return ReservationKind.STANDARD;
        }
        ReservationKind kind = request.getKind();
        if (kind == null || kind == ReservationKind.STANDARD) {
            return ReservationKind.INDIVIDUAL;
        }
        if (kind != ReservationKind.INDIVIDUAL && kind != ReservationKind.LESSON) {
            throw new ReservationException("kind", "Choose individual or lesson");
        }
        return kind;
    }

    private int resolvePartySize(SportResource resource, ReservationRequest request, ReservationKind kind) {
        if (kind == ReservationKind.LESSON && resource.requiresLessonPartySize()) {
            Integer partySize = request.getPartySize();
            if (partySize == null) {
                throw new ReservationException("partySize", "Choose how many people are coming");
            }
            int min = 2;
            int max = resource.getCapacity();
            if (partySize < min || partySize > max) {
                throw new ReservationException(
                        "partySize",
                        "Party size must be between " + min + " and " + max
                );
            }
            return partySize;
        }
        if (!resource.requiresPartySize()) {
            return 1;
        }
        Integer partySize = request.getPartySize();
        if (partySize == null) {
            throw new ReservationException("partySize", "Choose how many people are coming");
        }
        if (partySize < resource.getMinPartySize() || partySize > resource.getMaxPartySize()) {
            throw new ReservationException(
                    "partySize",
                    "Party size must be between " + resource.getMinPartySize() + " and " + resource.getMaxPartySize()
            );
        }
        return partySize;
    }

    private List<InventoryItem> resolveExtras(SportResource resource, List<Long> extraIds) {
        if (extraIds == null || extraIds.isEmpty()) {
            return List.of();
        }
        Map<Long, InventoryItem> unique = new LinkedHashMap<>();
        for (Long extraId : extraIds) {
            if (extraId == null || unique.containsKey(extraId)) {
                continue;
            }
            InventoryItem item = inventoryItemRepository.findById(extraId)
                    .orElseThrow(() -> new ReservationException("extraIds", "That extra is not available"));
            if (!item.isEnabled() || item.getResourceType() != resource.getType()) {
                throw new ReservationException("extraIds", "That extra is not available for this court");
            }
            unique.put(extraId, item);
        }
        return new ArrayList<>(unique.values());
    }

    private User applyBookingPhone(User user, ReservationRequest request) {
        boolean missing = user.getPhone() == null || user.getPhone().isBlank();
        if (!missing) {
            return user;
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new ReservationException("phone", "Phone is required to complete this booking");
        }
        String phone = request.getPhone().trim();
        if (!PHONE.matcher(phone).matches()) {
            throw new ReservationException("phone", "Enter a valid phone number");
        }
        return userService.updatePhone(user, phone);
    }

    private AssignedCoach resolveCoach(
            SportResource resource,
            ReservationKind kind,
            ReservationRequest request,
            int hours
    ) {
        Long coachId = request.getCoachId();
        if (coachId == null) {
            return null;
        }
        if (kind == ReservationKind.LESSON) {
            throw new ReservationException("coachId", "A coach cannot be added to a group lesson");
        }
        User coach;
        try {
            coach = userRepository.lockById(coachId)
                    .orElseThrow(() -> new ReservationException("coachId", "That coach could not be found"));
        } catch (TransientDataAccessException ex) {
            throw new ReservationException("coachId", "That coach is no longer available for this time");
        }
        if (coach.getRole() != Role.COACH || !coach.isEnabled()) {
            throw new ReservationException("coachId", "That coach could not be found");
        }
        String level = request.getSkillLevel() == null ? "" : request.getSkillLevel().trim();
        if (level.isEmpty()) {
            throw new ReservationException("skillLevel", "Choose your level for this sport");
        }
        if (!sportSkillLevelCatalog.isValid(resource.getType(), level)) {
            throw new ReservationException("skillLevel", "Choose a level that belongs to this sport");
        }
        CoachOffering offering = coachOfferingService.findByCoachAndSport(coach.getId(), resource.getType())
                .orElseThrow(() -> new ReservationException("coachId", "That coach does not teach this sport"));
        if (!offering.covers(level)) {
            throw new ReservationException("coachId", "That coach does not teach this level");
        }
        BigDecimal fee = offering.getPricePerHour()
                .multiply(BigDecimal.valueOf(hours))
                .setScale(2, RoundingMode.HALF_UP);
        return new AssignedCoach(coach, level, fee);
    }

    private boolean isAligned(SportResource resource, LocalTime startTime) {
        long minutes = Duration.between(resource.getOpeningTime(), startTime).toMinutes();
        return minutes >= 0 && minutes % resource.getSlotDurationMinutes() == 0;
    }

    private record AssignedCoach(User coach, String level, BigDecimal fee) {
    }
}

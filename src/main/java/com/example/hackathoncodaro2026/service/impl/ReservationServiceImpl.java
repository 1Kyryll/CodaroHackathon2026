package com.example.hackathoncodaro2026.service.impl;

import com.example.hackathoncodaro2026.dto.ReservationRequest;
import com.example.hackathoncodaro2026.dto.ReservationUpdateResult;
import com.example.hackathoncodaro2026.exception.ReservationException;
import com.example.hackathoncodaro2026.model.CoachOffering;
import com.example.hackathoncodaro2026.model.InventoryItem;
import com.example.hackathoncodaro2026.model.Reservation;
import com.example.hackathoncodaro2026.model.ReservationExtra;
import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.CancellationReason;
import com.example.hackathoncodaro2026.model.enums.NotificationType;
import com.example.hackathoncodaro2026.model.enums.ReservationKind;
import com.example.hackathoncodaro2026.model.enums.ReservationStatus;
import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.repository.CoachRatingRepository;
import com.example.hackathoncodaro2026.repository.InventoryItemRepository;
import com.example.hackathoncodaro2026.repository.ReservationExtraRepository;
import com.example.hackathoncodaro2026.repository.ReservationRepository;
import com.example.hackathoncodaro2026.repository.SportResourceRepository;
import com.example.hackathoncodaro2026.repository.UserRepository;
import com.example.hackathoncodaro2026.service.AuditLogService;
import com.example.hackathoncodaro2026.service.CoachOfferingService;
import com.example.hackathoncodaro2026.service.NotificationService;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final Pattern PHONE = Pattern.compile("^[+]?[0-9\\s().-]{7,20}$");
    private static final DateTimeFormatter RANGE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

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
            SportSkillLevelCatalog sportSkillLevelCatalog,
            AuditLogService auditLogService,
            NotificationService notificationService
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
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reservation create(User user, ReservationRequest request) {
        SportResource resource;
        try {
            resource = sportResourceRepository.lockById(request.getResourceId())
                    .orElseThrow(() -> failEx(user, "RESOURCE_NOT_FOUND", "resourceId", "That court could not be found"));
        } catch (TransientDataAccessException ex) {
            throw failEx(user, "LOCK_TIMEOUT", "This slot was just booked, pick another");
        }
        if (!resource.isEnabled() || resource.getFacility() == null || !resource.getFacility().isEnabled()) {
            throw failEx(user, "VENUE_CLOSED", "resourceId", "This venue is not open for booking");
        }
        if (request.getPaymentMethod() == null) {
            throw failEx(user, "PAYMENT_METHOD_REQUIRED", "paymentMethod", "Choose a payment method");
        }
        ReservationKind kind = resolveKind(resource, request, user);
        int partySize = resolvePartySize(resource, request, kind, user);
        List<InventoryItem> extras = resolveExtras(resource, request.getExtraIds(), user);
        LocalDateTime startAt = LocalDateTime.of(request.getDate(), request.getStartTime());
        int hours = request.getDurationHours() == null ? 1 : request.getDurationHours();
        if (hours < 1 || hours > 4) {
            throw failEx(user, "DURATION_RANGE", "durationHours", "Duration must be between 1 and 4 hours");
        }
        int durationMinutes = hours * 60;
        if (durationMinutes % resource.getSlotDurationMinutes() != 0) {
            throw failEx(
                    user,
                    "DURATION_SLOT",
                    "durationHours",
                    "Duration must be a multiple of " + resource.getSlotDurationMinutes() + " minutes"
            );
        }
        LocalDateTime endAt = startAt.plusMinutes(durationMinutes);
        LocalDateTime now = LocalDateTime.now(WARSAW);
        if (!startAt.isAfter(now)) {
            throw failEx(user, "PAST_SLOT", "startTime", "You cannot book a slot in the past");
        }
        if (!endAt.isAfter(startAt)) {
            throw failEx(user, "END_BEFORE_START", "startTime", "End time must be after start time");
        }
        if (!isAligned(resource, request.getStartTime())) {
            throw failEx(
                    user,
                    "START_NOT_ALIGNED",
                    "startTime",
                    "Start time must match a " + resource.getSlotDurationMinutes() + "-minute slot"
            );
        }
        if (request.getStartTime().isBefore(resource.getOpeningTime())
                || endAt.toLocalTime().isAfter(resource.getClosingTime())
                || endAt.toLocalDate().isAfter(request.getDate())) {
            throw failEx(user, "OUTSIDE_HOURS", "durationHours", "That duration sits outside opening hours");
        }
        User occupant = applyBookingPhone(user, request);
        AssignedCoach assignedCoach = resolveCoach(resource, kind, request, hours, occupant);
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
                    throw failEx(
                            occupant,
                            "LESSON_OCCUPIED",
                            "startTime",
                            "This lesson is not available because people are already coming"
                    );
                }
            } else if (booked >= resource.getCapacity()) {
                throw failEx(occupant, "CAPACITY_FULL", "startTime", "That slot is fully booked");
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
                throw failEx(occupant, "COACH_OVERLAP", "coachId", "That coach is no longer available for this time");
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
        Reservation saved;
        try {
            saved = reservationRepository.save(reservation);
        } catch (TransientDataAccessException ex) {
            throw failEx(occupant, "LOCK_TIMEOUT", "This slot was just booked, pick another");
        }
        auditCreated(saved, occupant, extras);
        return saved;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationUpdateResult update(User actor, Long reservationId, ReservationRequest request) {
        Reservation reservation;
        try {
            reservation = reservationRepository.lockById(reservationId)
                    .orElseThrow(() -> updateFail(actor, reservationId, "NOT_OWNER", "That reservation could not be found"));
            reservation.getUser().getId();
            reservation.getResource().getId();
            if (reservation.getResource().getFacility() != null) {
                reservation.getResource().getFacility().getId();
            }
            if (reservation.getCoach() != null) {
                reservation.getCoach().getId();
            }
            for (ReservationExtra extra : reservation.getExtras()) {
                if (extra.getItem() != null) {
                    extra.getItem().getId();
                }
            }
        } catch (TransientDataAccessException ex) {
            throw updateFail(actor, reservationId, "LOCK_TIMEOUT", "This reservation could not be updated, try again");
        }
        if (reservation.getUser() == null || actor == null || !reservation.getUser().getId().equals(actor.getId())) {
            throw updateFail(actor, reservationId, "NOT_OWNER", "That reservation could not be found");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw updateFail(actor, reservationId, "NOT_PENDING", "Only pending reservations can be changed.");
        }
        LocalDateTime now = LocalDateTime.now(WARSAW);
        if (!reservation.getStartAt().isAfter(now)) {
            throw updateFail(actor, reservationId, "NOT_PENDING", "This reservation can no longer be changed.");
        }
        SportResource resource;
        try {
            resource = sportResourceRepository.lockById(reservation.getResource().getId())
                    .orElseThrow(() -> updateFail(actor, reservationId, "VALIDATION", "That court could not be found"));
        } catch (TransientDataAccessException ex) {
            throw updateFail(actor, reservationId, "LOCK_TIMEOUT", "This reservation could not be updated, try again");
        }
        if (!resource.isEnabled() || resource.getFacility() == null || !resource.getFacility().isEnabled()) {
            throw updateFail(actor, reservationId, "VALIDATION", "resourceId", "This venue is not open for booking");
        }
        request.setResourceId(resource.getId());
        request.setKind(reservation.getKind());
        ReservationKind kind = reservation.getKind();
        if (request.getPaymentMethod() == null) {
            throw updateFail(actor, reservationId, "VALIDATION", "paymentMethod", "Choose a payment method");
        }
        int partySize = resolvePartySizeForUpdate(resource, request, kind, actor, reservationId);
        List<InventoryItem> extras = resolveExtrasForUpdate(resource, request.getExtraIds(), actor, reservationId);
        if (request.getDate() == null || request.getStartTime() == null) {
            throw updateFail(actor, reservationId, "INVALID_TIME", "startTime", "Choose a start time");
        }
        LocalDateTime startAt = LocalDateTime.of(request.getDate(), request.getStartTime());
        int hours = request.getDurationHours() == null ? 1 : request.getDurationHours();
        if (hours < 1 || hours > 4) {
            throw updateFail(actor, reservationId, "INVALID_TIME", "durationHours", "Duration must be between 1 and 4 hours");
        }
        int durationMinutes = hours * 60;
        if (durationMinutes % resource.getSlotDurationMinutes() != 0) {
            throw updateFail(
                    actor,
                    reservationId,
                    "INVALID_TIME",
                    "durationHours",
                    "Duration must be a multiple of " + resource.getSlotDurationMinutes() + " minutes"
            );
        }
        LocalDateTime endAt = startAt.plusMinutes(durationMinutes);
        if (!startAt.isAfter(now)) {
            throw updateFail(actor, reservationId, "INVALID_TIME", "startTime", "You cannot book a slot in the past");
        }
        if (!endAt.isAfter(startAt)) {
            throw updateFail(actor, reservationId, "INVALID_TIME", "startTime", "End time must be after start time");
        }
        if (!isAligned(resource, request.getStartTime())) {
            throw updateFail(
                    actor,
                    reservationId,
                    "INVALID_TIME",
                    "startTime",
                    "Start time must match a " + resource.getSlotDurationMinutes() + "-minute slot"
            );
        }
        if (request.getStartTime().isBefore(resource.getOpeningTime())
                || endAt.toLocalTime().isAfter(resource.getClosingTime())
                || endAt.toLocalDate().isAfter(request.getDate())) {
            throw updateFail(actor, reservationId, "INVALID_TIME", "durationHours", "That duration sits outside opening hours");
        }
        User occupant = applyBookingPhoneForUpdate(actor, request, reservationId);
        Long oldCoachId = reservation.getCoach() == null ? null : reservation.getCoach().getId();
        Long requestedCoachId = request.getCoachId();
        if (kind == ReservationKind.LESSON && requestedCoachId != null) {
            throw updateFail(actor, reservationId, "VALIDATION", "coachId", "A coach cannot be added to a group lesson");
        }
        Map<Long, User> lockedCoaches = lockCoachesInOrder(oldCoachId, requestedCoachId, occupant, reservationId);
        AssignedCoach assignedCoach = resolveCoachLocked(
                resource,
                kind,
                request,
                hours,
                occupant,
                reservationId,
                requestedCoachId == null ? null : lockedCoaches.get(requestedCoachId)
        );
        LocalTime cursor = request.getStartTime();
        while (cursor.isBefore(endAt.toLocalTime())) {
            LocalDateTime slotStart = LocalDateTime.of(request.getDate(), cursor);
            LocalDateTime slotEnd = slotStart.plusMinutes(resource.getSlotDurationMinutes());
            long booked = reservationRepository.countOverlappingExcluding(
                    resource.getId(),
                    ReservationStatus.occupying(),
                    slotStart,
                    slotEnd,
                    reservation.getId()
            );
            if (kind == ReservationKind.LESSON) {
                if (booked > 0) {
                    throw updateFail(
                            occupant,
                            reservationId,
                            "CAPACITY_FULL",
                            "startTime",
                            "This lesson is not available because people are already coming"
                    );
                }
            } else if (booked >= resource.getCapacity()) {
                throw updateFail(occupant, reservationId, "CAPACITY_FULL", "startTime", "That slot is fully booked");
            }
            cursor = cursor.plusMinutes(resource.getSlotDurationMinutes());
        }
        if (assignedCoach != null) {
            long coachBooked = reservationRepository.countCoachOverlappingExcluding(
                    assignedCoach.coach().getId(),
                    ReservationStatus.occupying(),
                    startAt,
                    endAt,
                    reservation.getId()
            );
            if (coachBooked > 0) {
                throw updateFail(occupant, reservationId, "COACH_OVERLAP", "coachId", "That coach is no longer available for this time");
            }
        }
        BigDecimal coachFee = assignedCoach == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : assignedCoach.fee();
        LocalDateTime oldStart = reservation.getStartAt();
        LocalDateTime oldEnd = reservation.getEndAt();
        BigDecimal oldAmount = reservation.getTotalAmount() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : reservation.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        User oldCoach = reservation.getCoach();
        int oldParty = reservation.getPartySize();
        String oldSkill = reservation.getSkillLevel();
        var oldPayment = reservation.getPaymentMethod();
        String oldNote = reservation.getNote();
        Set<Long> oldExtraIds = reservation.getExtras().stream()
                .map(ReservationExtra::getItem)
                .filter(Objects::nonNull)
                .map(InventoryItem::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        BigDecimal newAmount = pricingService.quote(
                resource,
                request.getDate(),
                request.getStartTime(),
                hours,
                kind,
                extras,
                partySize,
                coachFee
        );
        reservation.getExtras().clear();
        reservation.setStartAt(startAt);
        reservation.setEndAt(endAt);
        reservation.setPartySize(partySize);
        reservation.setPaymentMethod(request.getPaymentMethod());
        reservation.setKind(kind);
        reservation.setOccupancyUnits(kind == ReservationKind.LESSON ? resource.getCapacity() : 1);
        reservation.setStatus(ReservationStatus.PENDING);
        if (assignedCoach != null) {
            reservation.setCoach(assignedCoach.coach());
            reservation.setSkillLevel(assignedCoach.level());
        } else {
            reservation.setCoach(null);
            if (request.getSkillLevel() != null && !request.getSkillLevel().isBlank()
                    && sportSkillLevelCatalog.isValid(resource.getType(), request.getSkillLevel().trim())) {
                reservation.setSkillLevel(request.getSkillLevel().trim());
            } else {
                reservation.setSkillLevel(null);
            }
        }
        reservation.setTotalAmount(newAmount);
        if (request.getNote() != null && !request.getNote().isBlank()) {
            reservation.setNote(request.getNote().trim());
        } else {
            reservation.setNote(null);
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
        Reservation saved;
        try {
            saved = reservationRepository.save(reservation);
            reservationRepository.flush();
        } catch (TransientDataAccessException ex) {
            throw updateFail(occupant, reservationId, "LOCK_TIMEOUT", "This reservation could not be updated, try again");
        }
        Long newCoachId = saved.getCoach() == null ? null : saved.getCoach().getId();
        notifyReservationUpdated(occupant, saved, oldAmount, newAmount);
        notifyCoachesOfUpdate(oldCoach, saved.getCoach(), oldStart, oldEnd, saved.getStartAt(), saved.getEndAt(), saved.getId(), occupant);
        List<String> changed = changedFields(
                oldStart,
                oldEnd,
                saved.getStartAt(),
                saved.getEndAt(),
                oldAmount,
                newAmount,
                oldCoachId,
                newCoachId,
                oldParty,
                saved.getPartySize(),
                oldPayment,
                saved.getPaymentMethod(),
                oldSkill,
                saved.getSkillLevel(),
                oldNote,
                saved.getNote(),
                oldExtraIds,
                extras
        );
        auditUpdated(saved, occupant, oldStart, oldEnd, oldAmount, oldCoachId, changed);
        return new ReservationUpdateResult(saved, oldAmount, newAmount);
    }

    @Override
    @Transactional
    public void cancel(User actor, Long reservationId) {
        throw failEx(actor, "REASON_REQUIRED", "Choose a cancellation reason");
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
                .orElseThrow(() -> failEx(actor, "NOT_FOUND", "That reservation could not be found"));
        try {
            sportResourceRepository.lockById(reservation.getResource().getId())
                    .orElseThrow(() -> failEx(actor, "RESOURCE_NOT_FOUND", "That court could not be found"));
        } catch (TransientDataAccessException ex) {
            throw failEx(actor, "LOCK_TIMEOUT", "This reservation could not be updated, try again");
        }
        boolean owner = reservation.getUser().getId().equals(actor.getId());
        boolean staff = actor.getRole() == Role.ADMIN || actor.getRole() == Role.MANAGER;
        if (!owner && !staff) {
            throw failEx(actor, "NOT_OWNER", reservation.getId(), "You can only cancel your own reservation");
        }
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw failEx(actor, "CONFIRMED_NOT_CANCELLABLE", reservation.getId(), "Confirmed reservations cannot be cancelled");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw failEx(actor, "NOT_PENDING", reservation.getId(), "Only a pending reservation can be cancelled");
        }
        if (!reservation.getStartAt().isAfter(LocalDateTime.now(WARSAW))) {
            throw failEx(actor, "PAST_NOT_CANCELLABLE", reservation.getId(), "Past reservations cannot be cancelled");
        }
        CancellationReason parsed = CancellationReason.fromPosted(reason);
        if (parsed == null) {
            throw failEx(actor, "REASON_REQUIRED", reservation.getId(), "Choose a cancellation reason");
        }
        String stored = parsed.getLabel();
        boolean notePresent = false;
        if (parsed == CancellationReason.OTHER && otherNote != null && !otherNote.isBlank()) {
            String extra = otherNote.trim();
            if (extra.length() > 400) {
                throw failEx(actor, "NOTE_TOO_LONG", reservation.getId(), "Cancellation note must be 400 characters or fewer");
            }
            stored = parsed.getLabel() + ": " + extra;
            notePresent = true;
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancellationReason(stored);
        reservationRepository.save(reservation);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", parsed.name());
        details.put("notePresent", notePresent);
        auditLogService.record(actor, "RESERVATION_CANCEL", "RESERVATION", reservation.getId(), "SUCCESS", details);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void confirm(User actor, Long reservationId) {
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.MANAGER) {
            throw failEx(actor, "NOT_MANAGER", "Only a manager can confirm a reservation");
        }
        Reservation reservation = reservationRepository.findWithDetailsById(reservationId)
                .orElseThrow(() -> failEx(actor, "NOT_FOUND", "That reservation could not be found"));
        try {
            sportResourceRepository.lockById(reservation.getResource().getId())
                    .orElseThrow(() -> failEx(actor, "RESOURCE_NOT_FOUND", "That court could not be found"));
        } catch (TransientDataAccessException ex) {
            throw failEx(actor, "LOCK_TIMEOUT", "This reservation could not be updated, try again");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw failEx(actor, "NOT_PENDING", reservation.getId(), "Only a pending reservation can be confirmed");
        }
        if (!reservation.getStartAt().isAfter(LocalDateTime.now(WARSAW))) {
            throw failEx(actor, "PAST_NOT_CONFIRMABLE", reservation.getId(), "Past reservations cannot be confirmed");
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
        auditLogService.record(actor, "RESERVATION_CONFIRM", "RESERVATION", reservation.getId(), "SUCCESS", Map.of());
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
        int deleted = reservationRepository.deleteEndedBefore(cutoff);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("deleted", deleted);
        details.put("cutoff", cutoff == null ? "" : cutoff.toString());
        auditLogService.record(
                "RESERVATION_PURGE",
                "system",
                "SCHEDULER",
                "RESERVATION",
                null,
                "SUCCESS",
                details
        );
        return deleted;
    }

    @Override
    @Transactional
    public int deleteEndedOlderThanOneMonth() {
        return deleteEndedBefore(LocalDateTime.now(WARSAW).minusMonths(1));
    }

    @Override
    public Optional<Reservation> findWithDetails(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return reservationRepository.findWithDetailsById(id);
    }

    @Override
    public boolean canEdit(User actor, Reservation reservation) {
        if (actor == null || actor.getId() == null || reservation == null) {
            return false;
        }
        if (reservation.getUser() == null || !actor.getId().equals(reservation.getUser().getId())) {
            return false;
        }
        if (reservation.getStatus() != ReservationStatus.PENDING || reservation.getStartAt() == null) {
            return false;
        }
        return reservation.getStartAt().isAfter(LocalDateTime.now(WARSAW));
    }

    private ReservationKind resolveKind(SportResource resource, ReservationRequest request, User actor) {
        if (!resource.requiresBookingMode()) {
            return ReservationKind.STANDARD;
        }
        ReservationKind kind = request.getKind();
        if (kind == null || kind == ReservationKind.STANDARD) {
            return ReservationKind.INDIVIDUAL;
        }
        if (kind != ReservationKind.INDIVIDUAL && kind != ReservationKind.LESSON) {
            throw failEx(actor, "KIND_INVALID", "kind", "Choose individual or lesson");
        }
        return kind;
    }

    private int resolvePartySize(SportResource resource, ReservationRequest request, ReservationKind kind, User actor) {
        if (kind == ReservationKind.LESSON && resource.requiresLessonPartySize()) {
            Integer partySize = request.getPartySize();
            if (partySize == null) {
                throw failEx(actor, "PARTY_SIZE_REQUIRED", "partySize", "Choose how many people are coming");
            }
            int min = 2;
            int max = resource.getCapacity();
            if (partySize < min || partySize > max) {
                throw failEx(
                        actor,
                        "PARTY_SIZE_RANGE",
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
            throw failEx(actor, "PARTY_SIZE_REQUIRED", "partySize", "Choose how many people are coming");
        }
        if (partySize < resource.getMinPartySize() || partySize > resource.getMaxPartySize()) {
            throw failEx(
                    actor,
                    "PARTY_SIZE_RANGE",
                    "partySize",
                    "Party size must be between " + resource.getMinPartySize() + " and " + resource.getMaxPartySize()
            );
        }
        return partySize;
    }

    private List<InventoryItem> resolveExtras(SportResource resource, List<Long> extraIds, User actor) {
        if (extraIds == null || extraIds.isEmpty()) {
            return List.of();
        }
        Map<Long, InventoryItem> unique = new LinkedHashMap<>();
        for (Long extraId : extraIds) {
            if (extraId == null || unique.containsKey(extraId)) {
                continue;
            }
            InventoryItem item = inventoryItemRepository.findById(extraId)
                    .orElseThrow(() -> failEx(actor, "EXTRA_UNAVAILABLE", "extraIds", "That extra is not available"));
            if (!item.isEnabled() || item.getResourceType() != resource.getType()) {
                throw failEx(actor, "EXTRA_UNAVAILABLE", "extraIds", "That extra is not available for this court");
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
            throw failEx(user, "PHONE_REQUIRED", "phone", "Phone is required to complete this booking");
        }
        String phone = request.getPhone().trim();
        if (!PHONE.matcher(phone).matches()) {
            throw failEx(user, "PHONE_INVALID", "phone", "Enter a valid phone number");
        }
        return userService.updatePhone(user, phone);
    }

    private AssignedCoach resolveCoach(
            SportResource resource,
            ReservationKind kind,
            ReservationRequest request,
            int hours,
            User actor
    ) {
        Long coachId = request.getCoachId();
        if (coachId == null) {
            return null;
        }
        if (kind == ReservationKind.LESSON) {
            throw failEx(actor, "COACH_ON_LESSON", "coachId", "A coach cannot be added to a group lesson");
        }
        User coach;
        try {
            coach = userRepository.lockById(coachId)
                    .orElseThrow(() -> failEx(actor, "COACH_NOT_FOUND", "coachId", "That coach could not be found"));
        } catch (TransientDataAccessException ex) {
            throw failEx(actor, "LOCK_TIMEOUT", "coachId", "That coach is no longer available for this time");
        }
        if (coach.getRole() != Role.COACH || !coach.isEnabled()) {
            throw failEx(actor, "COACH_NOT_FOUND", "coachId", "That coach could not be found");
        }
        String level = request.getSkillLevel() == null ? "" : request.getSkillLevel().trim();
        if (level.isEmpty()) {
            throw failEx(actor, "SKILL_LEVEL_REQUIRED", "skillLevel", "Choose your level for this sport");
        }
        if (!sportSkillLevelCatalog.isValid(resource.getType(), level)) {
            throw failEx(actor, "SKILL_LEVEL_INVALID", "skillLevel", "Choose a level that belongs to this sport");
        }
        CoachOffering offering = coachOfferingService.findByCoachAndSport(coach.getId(), resource.getType())
                .orElseThrow(() -> failEx(actor, "COACH_SPORT_MISMATCH", "coachId", "That coach does not teach this sport"));
        if (!offering.covers(level)) {
            throw failEx(actor, "COACH_LEVEL_MISMATCH", "coachId", "That coach does not teach this level");
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

    private void auditCreated(Reservation saved, User actor, List<InventoryItem> extras) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (saved.getResource() != null) {
            details.put("resourceId", saved.getResource().getId());
        }
        details.put("startAt", saved.getStartAt());
        details.put("endAt", saved.getEndAt());
        details.put("status", saved.getStatus());
        details.put("partySize", saved.getPartySize());
        details.put("kind", saved.getKind());
        details.put("paymentMethod", saved.getPaymentMethod());
        details.put("totalPln", saved.getTotalAmount());
        if (saved.getCoach() != null) {
            details.put("coachId", saved.getCoach().getId());
        }
        if (extras != null && !extras.isEmpty()) {
            details.put(
                    "extraIds",
                    extras.stream().map(item -> String.valueOf(item.getId())).collect(Collectors.joining(","))
            );
            details.put(
                    "extraNames",
                    extras.stream().map(InventoryItem::getName).collect(Collectors.joining(","))
            );
        }
        auditLogService.record(actor, "RESERVATION_CREATE", "RESERVATION", saved.getId(), "SUCCESS", details);
    }

    private void auditUpdated(
            Reservation saved,
            User actor,
            LocalDateTime oldStart,
            LocalDateTime oldEnd,
            BigDecimal oldAmount,
            Long oldCoachId,
            List<String> changed
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (saved.getResource() != null) {
            details.put("resourceId", saved.getResource().getId());
        }
        details.put("oldStartAt", oldStart);
        details.put("newStartAt", saved.getStartAt());
        details.put("oldEndAt", oldEnd);
        details.put("newEndAt", saved.getEndAt());
        details.put("oldAmount", money(oldAmount));
        details.put("newAmount", money(saved.getTotalAmount()));
        details.put("oldCoachId", oldCoachId == null ? "" : oldCoachId);
        details.put("newCoachId", saved.getCoach() == null ? "" : saved.getCoach().getId());
        details.put("changedFields", String.join(",", changed));
        auditLogService.record(actor, "RESERVATION_UPDATE", "RESERVATION", saved.getId(), "SUCCESS", details);
    }

    private List<String> changedFields(
            LocalDateTime oldStart,
            LocalDateTime oldEnd,
            LocalDateTime newStart,
            LocalDateTime newEnd,
            BigDecimal oldAmount,
            BigDecimal newAmount,
            Long oldCoachId,
            Long newCoachId,
            int oldParty,
            int newParty,
            Object oldPayment,
            Object newPayment,
            String oldSkill,
            String newSkill,
            String oldNote,
            String newNote,
            Set<Long> oldExtraIds,
            List<InventoryItem> extras
    ) {
        List<String> changed = new ArrayList<>();
        if (!Objects.equals(oldStart, newStart)) {
            changed.add("startAt");
        }
        if (!Objects.equals(oldEnd, newEnd)) {
            changed.add("endAt");
        }
        if (oldAmount == null || newAmount == null || oldAmount.compareTo(newAmount) != 0) {
            changed.add("totalAmount");
        }
        if (!Objects.equals(oldCoachId, newCoachId)) {
            changed.add("coach");
        }
        if (oldParty != newParty) {
            changed.add("partySize");
        }
        if (!Objects.equals(oldPayment, newPayment)) {
            changed.add("paymentMethod");
        }
        if (!Objects.equals(blankToNull(oldSkill), blankToNull(newSkill))) {
            changed.add("skillLevel");
        }
        if (!Objects.equals(blankToNull(oldNote), blankToNull(newNote))) {
            changed.add("note");
        }
        Set<Long> newExtraIds = extras == null
                ? Set.of()
                : extras.stream().map(InventoryItem::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (!oldExtraIds.equals(newExtraIds)) {
            changed.add("extras");
        }
        return changed;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Map<Long, User> lockCoachesInOrder(Long oldCoachId, Long requestedCoachId, User actor, Long reservationId) {
        List<Long> coachIds = new ArrayList<>();
        if (oldCoachId != null) {
            coachIds.add(oldCoachId);
        }
        if (requestedCoachId != null && !requestedCoachId.equals(oldCoachId)) {
            coachIds.add(requestedCoachId);
        }
        Collections.sort(coachIds);
        Map<Long, User> locked = new LinkedHashMap<>();
        try {
            for (Long coachId : coachIds) {
                User coach = userRepository.lockById(coachId)
                        .orElseThrow(() -> updateFail(actor, reservationId, "VALIDATION", "coachId", "That coach could not be found"));
                locked.put(coachId, coach);
            }
        } catch (TransientDataAccessException ex) {
            throw updateFail(actor, reservationId, "LOCK_TIMEOUT", "coachId", "That coach is no longer available for this time");
        }
        return locked;
    }

    private AssignedCoach resolveCoachLocked(
            SportResource resource,
            ReservationKind kind,
            ReservationRequest request,
            int hours,
            User actor,
            Long reservationId,
            User coach
    ) {
        Long coachId = request.getCoachId();
        if (coachId == null) {
            return null;
        }
        if (kind == ReservationKind.LESSON) {
            throw updateFail(actor, reservationId, "VALIDATION", "coachId", "A coach cannot be added to a group lesson");
        }
        if (coach == null) {
            throw updateFail(actor, reservationId, "VALIDATION", "coachId", "That coach could not be found");
        }
        if (coach.getRole() != Role.COACH || !coach.isEnabled()) {
            throw updateFail(actor, reservationId, "VALIDATION", "coachId", "That coach could not be found");
        }
        String level = request.getSkillLevel() == null ? "" : request.getSkillLevel().trim();
        if (level.isEmpty()) {
            throw updateFail(actor, reservationId, "VALIDATION", "skillLevel", "Choose your level for this sport");
        }
        if (!sportSkillLevelCatalog.isValid(resource.getType(), level)) {
            throw updateFail(actor, reservationId, "VALIDATION", "skillLevel", "Choose a level that belongs to this sport");
        }
        CoachOffering offering = coachOfferingService.findByCoachAndSport(coach.getId(), resource.getType())
                .orElseThrow(() -> updateFail(actor, reservationId, "VALIDATION", "coachId", "That coach does not teach this sport"));
        if (!offering.covers(level)) {
            throw updateFail(actor, reservationId, "VALIDATION", "coachId", "That coach does not teach this level");
        }
        BigDecimal fee = offering.getPricePerHour()
                .multiply(BigDecimal.valueOf(hours))
                .setScale(2, RoundingMode.HALF_UP);
        return new AssignedCoach(coach, level, fee);
    }

    private int resolvePartySizeForUpdate(
            SportResource resource,
            ReservationRequest request,
            ReservationKind kind,
            User actor,
            Long reservationId
    ) {
        if (kind == ReservationKind.LESSON && resource.requiresLessonPartySize()) {
            Integer partySize = request.getPartySize();
            if (partySize == null) {
                throw updateFail(actor, reservationId, "VALIDATION", "partySize", "Choose how many people are coming");
            }
            int min = 2;
            int max = resource.getCapacity();
            if (partySize < min || partySize > max) {
                throw updateFail(
                        actor,
                        reservationId,
                        "VALIDATION",
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
            throw updateFail(actor, reservationId, "VALIDATION", "partySize", "Choose how many people are coming");
        }
        if (partySize < resource.getMinPartySize() || partySize > resource.getMaxPartySize()) {
            throw updateFail(
                    actor,
                    reservationId,
                    "VALIDATION",
                    "partySize",
                    "Party size must be between " + resource.getMinPartySize() + " and " + resource.getMaxPartySize()
            );
        }
        return partySize;
    }

    private List<InventoryItem> resolveExtrasForUpdate(SportResource resource, List<Long> extraIds, User actor, Long reservationId) {
        if (extraIds == null || extraIds.isEmpty()) {
            return List.of();
        }
        Map<Long, InventoryItem> unique = new LinkedHashMap<>();
        for (Long extraId : extraIds) {
            if (extraId == null || unique.containsKey(extraId)) {
                continue;
            }
            InventoryItem item = inventoryItemRepository.findById(extraId)
                    .orElseThrow(() -> updateFail(actor, reservationId, "VALIDATION", "extraIds", "That extra is not available"));
            if (!item.isEnabled() || item.getResourceType() != resource.getType()) {
                throw updateFail(actor, reservationId, "VALIDATION", "extraIds", "That extra is not available for this court");
            }
            unique.put(extraId, item);
        }
        return new ArrayList<>(unique.values());
    }

    private User applyBookingPhoneForUpdate(User user, ReservationRequest request, Long reservationId) {
        boolean missing = user.getPhone() == null || user.getPhone().isBlank();
        if (!missing) {
            return user;
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw updateFail(user, reservationId, "VALIDATION", "phone", "Phone is required to complete this booking");
        }
        String phone = request.getPhone().trim();
        if (!PHONE.matcher(phone).matches()) {
            throw updateFail(user, reservationId, "VALIDATION", "phone", "Enter a valid phone number");
        }
        return userService.updatePhone(user, phone);
    }

    private void notifyReservationUpdated(User occupant, Reservation saved, BigDecimal oldAmount, BigDecimal newAmount) {
        String oldMoney = money(oldAmount);
        String newMoney = money(newAmount);
        String message;
        if (oldAmount != null && newAmount != null && oldAmount.compareTo(newAmount) == 0) {
            message = "Your reservation was updated. Amount remains " + newMoney + ".";
        } else {
            message = "Your reservation was updated. Amount changed from " + oldMoney + " to " + newMoney + ".";
        }
        notificationService.create(
                occupant,
                NotificationType.RESERVATION_UPDATED,
                "Reservation updated",
                message,
                saved.getId()
        );
    }

    private void notifyCoachesOfUpdate(
            User oldCoach,
            User newCoach,
            LocalDateTime oldStart,
            LocalDateTime oldEnd,
            LocalDateTime newStart,
            LocalDateTime newEnd,
            Long reservationId,
            User actor
    ) {
        Long oldId = oldCoach == null ? null : oldCoach.getId();
        Long newId = newCoach == null ? null : newCoach.getId();
        boolean sameCoach = oldId != null && oldId.equals(newId);
        boolean timeChanged = !Objects.equals(oldStart, newStart) || !Objects.equals(oldEnd, newEnd);
        if (sameCoach && timeChanged) {
            notificationService.create(
                    newCoach,
                    NotificationType.COACH_SCHEDULE_CHANGED,
                    "Booking time changed",
                    "A booking you are assigned to moved from " + rangeLabel(oldStart, oldEnd)
                            + " to " + rangeLabel(newStart, newEnd) + ".",
                    reservationId
            );
            auditCoachNotice(actor, reservationId, "COACH_SCHEDULE_CHANGED", newId);
            return;
        }
        if (oldCoach != null && !sameCoach) {
            notificationService.create(
                    oldCoach,
                    NotificationType.COACH_REMOVED,
                    "Removed from a booking",
                    "You were removed from a booking on " + rangeLabel(oldStart, oldEnd)
                            + " and are now available for that time.",
                    reservationId
            );
            auditCoachNotice(actor, reservationId, "COACH_REMOVED", oldId);
        }
        if (newCoach != null && !sameCoach) {
            notificationService.create(
                    newCoach,
                    NotificationType.COACH_ASSIGNED,
                    "Assigned to a booking",
                    "You were assigned to a booking on " + rangeLabel(newStart, newEnd) + ".",
                    reservationId
            );
            auditCoachNotice(actor, reservationId, "COACH_ASSIGNED", newId);
        }
    }

    private void auditCoachNotice(User actor, Long reservationId, String type, Long coachId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("noticeType", type);
        details.put("coachId", coachId);
        auditLogService.record(actor, "COACH_NOTIFICATION", "RESERVATION", reservationId, "SUCCESS", details);
    }

    private String rangeLabel(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "the booked time";
        }
        return start.format(RANGE_TIME) + "-" + end.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String money(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " PLN";
    }

    private ReservationException updateFail(User actor, Long reservationId, String reason, String message) {
        return updateFail(actor, reservationId, reason, "", message);
    }

    private ReservationException updateFail(User actor, Long reservationId, String reason, String field, String message) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", reason);
        if (fieldLooksLikeFormField(field)) {
            details.put("field", field);
        }
        auditLogService.record(actor, "RESERVATION_UPDATE", "RESERVATION", reservationId, "REJECTED", details);
        if (fieldLooksLikeFormField(field)) {
            return new ReservationException(field, message);
        }
        return new ReservationException(message);
    }

    private ReservationException failEx(User actor, String reason, String message) {
        return failEx(actor, reason, "", message);
    }

    private ReservationException failEx(User actor, String reason, String field, String message) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", reason);
        if (fieldLooksLikeFormField(field)) {
            details.put("field", field);
        }
        auditLogService.record(actor, "RESERVATION_REJECTED", "RESERVATION", null, "REJECTED", details);
        if (fieldLooksLikeFormField(field)) {
            return new ReservationException(field, message);
        }
        return new ReservationException(message);
    }

    private ReservationException failEx(User actor, String reason, Long reservationId, String message) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", reason);
        auditLogService.record(actor, "RESERVATION_REJECTED", "RESERVATION", reservationId, "REJECTED", details);
        return new ReservationException(message);
    }

    private boolean fieldLooksLikeFormField(String value) {
        return "resourceId".equals(value)
                || "paymentMethod".equals(value)
                || "durationHours".equals(value)
                || "startTime".equals(value)
                || "partySize".equals(value)
                || "kind".equals(value)
                || "extraIds".equals(value)
                || "phone".equals(value)
                || "coachId".equals(value)
                || "skillLevel".equals(value);
    }

    private record AssignedCoach(User coach, String level, BigDecimal fee) {
    }
}

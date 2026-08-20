package com.example.hackathoncodaro2026.controller;

import com.example.hackathoncodaro2026.dto.CoachRatingRequest;
import com.example.hackathoncodaro2026.dto.ReservationRequest;
import com.example.hackathoncodaro2026.dto.ReservationUpdateResult;
import com.example.hackathoncodaro2026.exception.ReservationException;
import com.example.hackathoncodaro2026.model.CoachRating;
import com.example.hackathoncodaro2026.model.InventoryItem;
import com.example.hackathoncodaro2026.model.Reservation;
import com.example.hackathoncodaro2026.model.ReservationExtra;
import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.CancellationReason;
import com.example.hackathoncodaro2026.model.enums.PaymentMethod;
import com.example.hackathoncodaro2026.model.enums.ReservationKind;
import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.repository.InventoryItemRepository;
import com.example.hackathoncodaro2026.service.CoachOfferingService;
import com.example.hackathoncodaro2026.service.CoachRatingService;
import com.example.hackathoncodaro2026.service.ReservationService;
import com.example.hackathoncodaro2026.service.ResourceService;
import com.example.hackathoncodaro2026.service.SportSkillLevelCatalog;
import com.example.hackathoncodaro2026.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Controller
public class ReservationController {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final ReservationService reservationService;
    private final UserService userService;
    private final CoachRatingService coachRatingService;
    private final ResourceService resourceService;
    private final InventoryItemRepository inventoryItemRepository;
    private final CoachOfferingService coachOfferingService;
    private final SportSkillLevelCatalog sportSkillLevelCatalog;

    public ReservationController(
            ReservationService reservationService,
            UserService userService,
            CoachRatingService coachRatingService,
            ResourceService resourceService,
            InventoryItemRepository inventoryItemRepository,
            CoachOfferingService coachOfferingService,
            SportSkillLevelCatalog sportSkillLevelCatalog
    ) {
        this.reservationService = reservationService;
        this.userService = userService;
        this.coachRatingService = coachRatingService;
        this.resourceService = resourceService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.coachOfferingService = coachOfferingService;
        this.sportSkillLevelCatalog = sportSkillLevelCatalog;
    }

    @GetMapping({"/reservations", "/history"})
    public String history(Authentication authentication, Model model) {
        User user = requireUser(authentication);
        boolean adminView = user.getRole() == Role.ADMIN;
        List<Reservation> reservations = adminView ? reservationService.findAll() : reservationService.findForUser(user);
        List<Long> reservationIds = reservations.stream().map(Reservation::getId).toList();
        Map<Long, CoachRating> ratings = coachRatingService.findByReservationIds(reservationIds);
        Set<Long> rateableIds = new HashSet<>();
        Set<Long> editableIds = new HashSet<>();
        for (Reservation reservation : reservations) {
            if (coachRatingService.canRate(user, reservation)) {
                rateableIds.add(reservation.getId());
            }
            if (reservationService.canEdit(user, reservation)) {
                editableIds.add(reservation.getId());
            }
        }
        model.addAttribute("reservations", reservations);
        model.addAttribute("adminView", adminView);
        model.addAttribute("now", LocalDateTime.now(WARSAW));
        model.addAttribute("cancelReasons", CancellationReason.values());
        model.addAttribute("coachRatings", ratings);
        model.addAttribute("rateableIds", rateableIds);
        model.addAttribute("editableIds", editableIds);
        return "reservations/history";
    }

    @GetMapping("/reservations/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User user = requireUser(authentication);
        Reservation reservation = reservationService.findWithDetails(id).orElse(null);
        if (reservation == null || reservation.getUser() == null || !reservation.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "That reservation could not be found");
            return "redirect:/reservations";
        }
        if (!reservationService.canEdit(user, reservation)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only pending reservations can be changed.");
            return "redirect:/reservations";
        }
        ReservationRequest reservationRequest = toRequest(reservation, date);
        populateEditModel(model, reservation, reservationRequest, user);
        return "reservations/edit";
    }

    @PostMapping("/reservations/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("reservationRequest") ReservationRequest reservationRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User user = requireUser(authentication);
        Reservation reservation = reservationService.findWithDetails(id).orElse(null);
        if (reservation == null || reservation.getUser() == null || !reservation.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "That reservation could not be found");
            return "redirect:/reservations";
        }
        if (!reservationService.canEdit(user, reservation)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only pending reservations can be changed.");
            return "redirect:/reservations";
        }
        reservationRequest.setResourceId(reservation.getResource().getId());
        reservationRequest.setKind(reservation.getKind());
        if (needsPhone(user) && (reservationRequest.getPhone() == null || reservationRequest.getPhone().isBlank())) {
            bindingResult.rejectValue("phone", "required", "Phone is required to complete this booking");
        }
        if (bindingResult.hasErrors()) {
            populateEditModel(model, reservation, reservationRequest, user);
            model.addAttribute("errorMessage", firstError(bindingResult));
            return "reservations/edit";
        }
        try {
            ReservationUpdateResult result = reservationService.update(user, id, reservationRequest);
            redirectAttributes.addFlashAttribute("successMessage", amountFlash(result));
            return "redirect:/reservations";
        } catch (ReservationException ex) {
            if (ex.getField() == null || ex.getField().isBlank()) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/reservations";
            }
            bindingResult.rejectValue(ex.getField(), "invalid", ex.getMessage());
            populateEditModel(model, reservation, reservationRequest, user);
            model.addAttribute("errorMessage", ex.getMessage());
            return "reservations/edit";
        }
    }

    @PostMapping("/reservations")
    public String create(
            @Valid @ModelAttribute("reservationRequest") ReservationRequest reservationRequest,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = requireUser(authentication);
        if (reservationRequest.getResourceId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Choose a court before booking.");
            return "redirect:/facilities";
        }
        if (needsPhone(user) && (reservationRequest.getPhone() == null || reservationRequest.getPhone().isBlank())) {
            bindingResult.rejectValue("phone", "required", "Phone is required to complete this booking");
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return redirectToResource(reservationRequest);
        }
        try {
            reservationService.create(user, reservationRequest);
        } catch (ReservationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return redirectToResource(reservationRequest);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Thank you for your reservation");
        return "redirect:/reservations";
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String otherNote,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = requireUser(authentication);
        try {
            reservationService.cancel(user, id, reason, otherNote);
            redirectAttributes.addFlashAttribute("successMessage", "Reservation cancelled.");
        } catch (ReservationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/reservations";
    }

    @PostMapping("/reservations/{id}/coach-rating")
    public String rateCoach(
            @PathVariable Long id,
            @Valid @ModelAttribute("coachRatingRequest") CoachRatingRequest coachRatingRequest,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = requireUser(authentication);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return "redirect:/reservations";
        }
        try {
            coachRatingService.rate(user, id, coachRatingRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Thanks for rating your coach.");
        } catch (ReservationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/reservations";
    }

    private void populateEditModel(Model model, Reservation reservation, ReservationRequest reservationRequest, User user) {
        SportResource resource = reservation.getResource();
        ReservationKind kind = reservation.getKind();
        LocalDate selected = reservationRequest.getDate();
        LocalDate today = LocalDate.now(WARSAW);
        if (selected == null || selected.isBefore(today)) {
            selected = reservation.getStartAt().toLocalDate();
            if (selected.isBefore(today)) {
                selected = today;
            }
            reservationRequest.setDate(selected);
        }
        reservationRequest.setResourceId(resource.getId());
        reservationRequest.setKind(kind);
        boolean phoneRequired = needsPhone(user);
        boolean allowCoach = kind != ReservationKind.LESSON;
        model.addAttribute("reservation", reservation);
        model.addAttribute("resource", resource);
        model.addAttribute("facility", resource.getFacility());
        model.addAttribute("today", today);
        model.addAttribute("selectedDate", selected);
        model.addAttribute("reservationRequest", reservationRequest);
        model.addAttribute("phoneRequired", phoneRequired);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("inventoryItems", inventoryItemRepository.findByResourceTypeAndEnabledTrueOrderByNameAsc(resource.getType()));
        model.addAttribute("bookingKind", kind);
        model.addAttribute("allowCoach", allowCoach);
        model.addAttribute("slots", resourceService.slotsFor(resource, selected, kind, reservation.getId()));
        if (allowCoach) {
            model.addAttribute("sportLevels", sportSkillLevelCatalog.levelsFor(resource.getType()));
            model.addAttribute("levelGroupLabel", sportSkillLevelCatalog.groupLabel(resource.getType()));
            model.addAttribute("coachCards", coachOfferingService.pickerCards(resource.getType()));
        }
    }

    private ReservationRequest toRequest(Reservation reservation, LocalDate requestedDate) {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(reservation.getResource().getId());
        request.setKind(reservation.getKind());
        LocalDate today = LocalDate.now(WARSAW);
        LocalDate reservationDate = reservation.getStartAt().toLocalDate();
        LocalDate selected = requestedDate == null || requestedDate.isBefore(today) ? reservationDate : requestedDate;
        if (selected.isBefore(today)) {
            selected = today;
        }
        request.setDate(selected);
        if (selected.equals(reservationDate)) {
            request.setStartTime(reservation.getStartAt().toLocalTime());
        }
        int hours = (int) Duration.between(reservation.getStartAt(), reservation.getEndAt()).toHours();
        if (hours < 1) {
            hours = 1;
        }
        if (hours > 4) {
            hours = 4;
        }
        request.setDurationHours(hours);
        request.setPartySize(reservation.getPartySize());
        request.setPaymentMethod(reservation.getPaymentMethod());
        request.setNote(reservation.getNote());
        request.setSkillLevel(reservation.getSkillLevel());
        if (reservation.getCoach() != null) {
            request.setCoachId(reservation.getCoach().getId());
        }
        List<Long> extraIds = reservation.getExtras().stream()
                .map(ReservationExtra::getItem)
                .filter(Objects::nonNull)
                .map(InventoryItem::getId)
                .toList();
        request.setExtraIds(new ArrayList<>(extraIds));
        return request;
    }

    private String amountFlash(ReservationUpdateResult result) {
        String newAmount = money(result.newAmount());
        if (result.amountChanged()) {
            return "Reservation updated. Amount changed from " + money(result.previousAmount()) + " to " + newAmount + ".";
        }
        return "Reservation updated. Amount remains " + newAmount + ".";
    }

    private String money(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " PLN";
    }

    private boolean needsPhone(User user) {
        return user.getPhone() == null || user.getPhone().isBlank();
    }

    private User requireUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ReservationException("Signed-in user was not found"));
    }

    private String redirectToResource(ReservationRequest request) {
        String target = "redirect:/resources/" + request.getResourceId();
        List<String> query = new ArrayList<>();
        if (request.getDate() != null) {
            query.add("date=" + request.getDate());
        }
        if (request.getKind() != null) {
            query.add("mode=" + request.getKind().name());
        }
        if (!query.isEmpty()) {
            target += "?" + String.join("&", query);
        }
        return target;
    }

    private String firstError(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            return fieldError.getDefaultMessage();
        }
        if (bindingResult.getGlobalError() != null && bindingResult.getGlobalError().getDefaultMessage() != null) {
            return bindingResult.getGlobalError().getDefaultMessage();
        }
        return "Please check the booking form.";
    }
}

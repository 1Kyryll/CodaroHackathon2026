package com.example.hackathoncodaro2026.controller;

import com.example.hackathoncodaro2026.dto.CoachRatingRequest;
import com.example.hackathoncodaro2026.dto.ReservationRequest;
import com.example.hackathoncodaro2026.exception.ReservationException;
import com.example.hackathoncodaro2026.model.CoachRating;
import com.example.hackathoncodaro2026.model.Reservation;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.CancellationReason;
import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.service.CoachRatingService;
import com.example.hackathoncodaro2026.service.ReservationService;
import com.example.hackathoncodaro2026.service.UserService;
import jakarta.validation.Valid;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class ReservationController {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final ReservationService reservationService;
    private final UserService userService;
    private final CoachRatingService coachRatingService;

    public ReservationController(
            ReservationService reservationService,
            UserService userService,
            CoachRatingService coachRatingService
    ) {
        this.reservationService = reservationService;
        this.userService = userService;
        this.coachRatingService = coachRatingService;
    }

    @GetMapping({"/reservations", "/history"})
    public String history(Authentication authentication, Model model) {
        User user = requireUser(authentication);
        boolean adminView = user.getRole() == Role.ADMIN;
        List<Reservation> reservations = adminView ? reservationService.findAll() : reservationService.findForUser(user);
        List<Long> reservationIds = reservations.stream().map(Reservation::getId).toList();
        Map<Long, CoachRating> ratings = coachRatingService.findByReservationIds(reservationIds);
        Set<Long> rateableIds = new HashSet<>();
        for (Reservation reservation : reservations) {
            if (coachRatingService.canRate(user, reservation)) {
                rateableIds.add(reservation.getId());
            }
        }
        model.addAttribute("reservations", reservations);
        model.addAttribute("adminView", adminView);
        model.addAttribute("now", LocalDateTime.now(WARSAW));
        model.addAttribute("cancelReasons", CancellationReason.values());
        model.addAttribute("coachRatings", ratings);
        model.addAttribute("rateableIds", rateableIds);
        return "reservations/history";
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

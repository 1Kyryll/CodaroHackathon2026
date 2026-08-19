package com.example.hackathoncodaro2026.controller;

import com.example.hackathoncodaro2026.dto.PriceQuote;
import com.example.hackathoncodaro2026.dto.ReservationRequest;
import com.example.hackathoncodaro2026.model.InventoryItem;
import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.UserSportLevel;
import com.example.hackathoncodaro2026.model.enums.PaymentMethod;
import com.example.hackathoncodaro2026.model.enums.ReservationKind;
import com.example.hackathoncodaro2026.repository.InventoryItemRepository;
import com.example.hackathoncodaro2026.repository.UserSportLevelRepository;
import com.example.hackathoncodaro2026.service.CoachOfferingService;
import com.example.hackathoncodaro2026.service.PricingService;
import com.example.hackathoncodaro2026.service.ResourceService;
import com.example.hackathoncodaro2026.service.SportSkillLevelCatalog;
import com.example.hackathoncodaro2026.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ResourceController {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final ResourceService resourceService;
    private final UserService userService;
    private final PricingService pricingService;
    private final InventoryItemRepository inventoryItemRepository;
    private final CoachOfferingService coachOfferingService;
    private final SportSkillLevelCatalog sportSkillLevelCatalog;
    private final UserSportLevelRepository userSportLevelRepository;

    public ResourceController(
            ResourceService resourceService,
            UserService userService,
            PricingService pricingService,
            InventoryItemRepository inventoryItemRepository,
            CoachOfferingService coachOfferingService,
            SportSkillLevelCatalog sportSkillLevelCatalog,
            UserSportLevelRepository userSportLevelRepository
    ) {
        this.resourceService = resourceService;
        this.userService = userService;
        this.pricingService = pricingService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.coachOfferingService = coachOfferingService;
        this.sportSkillLevelCatalog = sportSkillLevelCatalog;
        this.userSportLevelRepository = userSportLevelRepository;
    }

    @GetMapping("/resources/{id}")
    public String detail(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam(required = false) String mode,
            Authentication authentication,
            Model model
    ) {
        SportResource resource = resourceService.findEnabledWithFacility(id).orElse(null);
        if (resource == null) {
            return "redirect:/facilities";
        }
        LocalDate today = LocalDate.now(WARSAW);
        LocalDate selected = date == null || date.isBefore(today) ? today : date;
        model.addAttribute("resource", resource);
        model.addAttribute("facility", resource.getFacility());
        model.addAttribute("today", today);
        model.addAttribute("selectedDate", selected);
        model.addAttribute("selectedStart", start);
        if (resource.requiresBookingMode()) {
            ReservationKind kind = parseMode(mode);
            if (kind == null || kind == ReservationKind.STANDARD) {
                return "resources/mode";
            }
            return bookingPage(resource, selected, start, kind, authentication, model);
        }
        return bookingPage(resource, selected, start, ReservationKind.STANDARD, authentication, model);
    }

    @GetMapping(value = "/resources/{id}/quote", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PriceQuote quote(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam Integer durationHours,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) List<Long> extraIds,
            @RequestParam(required = false) Integer people,
            @RequestParam(required = false) Long coachId
    ) {
        SportResource resource = resourceService.findEnabledWithFacility(id).orElse(null);
        if (resource == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (date == null || start == null || durationHours == null || durationHours < 1 || durationHours > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        ReservationKind resolved = parseMode(kind);
        if (resource.requiresBookingMode()) {
            if (resolved == null) {
                resolved = ReservationKind.INDIVIDUAL;
            }
        } else {
            resolved = ReservationKind.STANDARD;
        }
        List<InventoryItem> extras = resolveQuoteExtras(resource, extraIds);
        int heads = resolveQuotePeople(resource, resolved, people);
        BigDecimal coachFee = BigDecimal.ZERO;
        if (coachId != null && resolved != ReservationKind.LESSON) {
            coachFee = coachOfferingService.feeFor(coachId, resource.getType(), durationHours);
        }
        return new PriceQuote(
                pricingService.quote(resource, date, start, durationHours, resolved, extras, heads, coachFee),
                "PLN"
        );
    }

    private String bookingPage(
            SportResource resource,
            LocalDate selected,
            LocalTime start,
            ReservationKind kind,
            Authentication authentication,
            Model model
    ) {
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setResourceId(resource.getId());
        reservationRequest.setDate(selected);
        reservationRequest.setDurationHours(1);
        reservationRequest.setKind(kind);
        if (start != null) {
            reservationRequest.setStartTime(start);
        }
        User user = authentication == null ? null : userService.findByUsername(authentication.getName()).orElse(null);
        boolean phoneRequired = user == null || user.getPhone() == null || user.getPhone().isBlank();
        if (user != null && kind != ReservationKind.LESSON) {
            userSportLevelRepository.findByUser_IdAndSportType(user.getId(), resource.getType())
                    .map(UserSportLevel::getSkillLevel)
                    .ifPresent(reservationRequest::setSkillLevel);
        }
        model.addAttribute("slots", resourceService.slotsFor(resource, selected, kind));
        model.addAttribute("reservationRequest", reservationRequest);
        model.addAttribute("phoneRequired", phoneRequired);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("inventoryItems", inventoryItemRepository.findByResourceTypeAndEnabledTrueOrderByNameAsc(resource.getType()));
        model.addAttribute("bookingKind", kind);
        boolean allowCoach = kind != ReservationKind.LESSON;
        model.addAttribute("allowCoach", allowCoach);
        if (allowCoach) {
            model.addAttribute("sportLevels", sportSkillLevelCatalog.levelsFor(resource.getType()));
            model.addAttribute("levelGroupLabel", sportSkillLevelCatalog.groupLabel(resource.getType()));
            model.addAttribute("coachCards", coachOfferingService.pickerCards(resource.getType()));
        }
        return "resources/detail";
    }

    private ReservationKind parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return null;
        }
        try {
            return ReservationKind.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private int resolveQuotePeople(SportResource resource, ReservationKind kind, Integer people) {
        if (!resource.requiresAttendeeCount(kind)) {
            return 1;
        }
        int min = resource.attendeeMin(kind);
        int max = resource.attendeeMax(kind);
        int heads = people == null || people < 1 ? min : people;
        if (heads < min) {
            heads = min;
        }
        if (heads > max) {
            heads = max;
        }
        return heads;
    }

    private List<InventoryItem> resolveQuoteExtras(SportResource resource, List<Long> extraIds) {
        if (extraIds == null || extraIds.isEmpty()) {
            return List.of();
        }
        List<InventoryItem> extras = new ArrayList<>();
        for (Long extraId : extraIds) {
            if (extraId == null) {
                continue;
            }
            inventoryItemRepository.findById(extraId).ifPresent(item -> {
                if (item.isEnabled() && item.getResourceType() == resource.getType()) {
                    extras.add(item);
                }
            });
        }
        return extras;
    }
}

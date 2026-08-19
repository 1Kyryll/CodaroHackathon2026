package com.example.hackathoncodaro2026.controller;

import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.service.FacilityService;
import com.example.hackathoncodaro2026.service.OccupancyService;
import com.example.hackathoncodaro2026.service.ReservationService;
import com.example.hackathoncodaro2026.service.ResourceService;
import com.example.hackathoncodaro2026.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.ZoneId;

@Controller
public class HomeController {

    private final UserService userService;
    private final FacilityService facilityService;
    private final ResourceService resourceService;
    private final ReservationService reservationService;
    private final OccupancyService occupancyService;

    public HomeController(
            UserService userService,
            FacilityService facilityService,
            ResourceService resourceService,
            ReservationService reservationService,
            OccupancyService occupancyService
    ) {
        this.userService = userService;
        this.facilityService = facilityService;
        this.resourceService = resourceService;
        this.reservationService = reservationService;
        this.occupancyService = occupancyService;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        model.addAttribute("facilityCount", facilityService.countEnabled());
        model.addAttribute("resourceCount", resourceService.countEnabled());
        model.addAttribute("occupancyPercent", occupancyService.gridFor(LocalDate.now(ZoneId.of("Europe/Warsaw")), null).getFillPercent());
        long upcoming = 0;
        if (authentication != null) {
            User user = userService.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                upcoming = reservationService.countUpcomingActive(user);
                if (user.getRole() == Role.COACH) {
                    upcoming += reservationService.countUpcomingAsCoach(user);
                }
            }
        }
        model.addAttribute("upcomingCount", upcoming);
        return "home/index";
    }
}

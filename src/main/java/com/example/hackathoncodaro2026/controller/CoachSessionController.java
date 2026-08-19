package com.example.hackathoncodaro2026.controller;

import com.example.hackathoncodaro2026.exception.ReservationException;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.service.ReservationService;
import com.example.hackathoncodaro2026.service.SportSkillLevelCatalog;
import com.example.hackathoncodaro2026.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Controller
public class CoachSessionController {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final ReservationService reservationService;
    private final UserService userService;
    private final SportSkillLevelCatalog sportSkillLevelCatalog;

    public CoachSessionController(
            ReservationService reservationService,
            UserService userService,
            SportSkillLevelCatalog sportSkillLevelCatalog
    ) {
        this.reservationService = reservationService;
        this.userService = userService;
        this.sportSkillLevelCatalog = sportSkillLevelCatalog;
    }

    @GetMapping("/coach/sessions")
    public String coachSessions(Authentication authentication, Model model) {
        User coach = requireCoach(authentication);
        model.addAttribute("sessions", reservationService.findForCoach(coach));
        model.addAttribute("now", LocalDateTime.now(WARSAW));
        model.addAttribute("sportSkillLevelCatalog", sportSkillLevelCatalog);
        return "coach/sessions";
    }

    private User requireCoach(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ReservationException("Signed-in user was not found"));
        if (user.getRole() != Role.COACH) {
            throw new ReservationException("Only a coach can open this list");
        }
        return user;
    }
}

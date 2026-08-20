package com.example.hackathoncodaro2026.controller;

import com.example.hackathoncodaro2026.exception.ReservationException;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.service.NotificationService;
import com.example.hackathoncodaro2026.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping("/notifications")
    public String list(Authentication authentication, Model model) {
        User user = requireUser(authentication);
        model.addAttribute("notifications", notificationService.findFor(user));
        return "notifications/list";
    }

    @PostMapping("/notifications/{id}/read")
    public String markRead(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = requireUser(authentication);
        try {
            notificationService.markRead(user, id);
        } catch (ReservationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead(Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = requireUser(authentication);
        notificationService.markAllRead(user);
        redirectAttributes.addFlashAttribute("successMessage", "All notices are marked as read.");
        return "redirect:/notifications";
    }

    private User requireUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ReservationException("Signed-in user was not found"));
    }
}

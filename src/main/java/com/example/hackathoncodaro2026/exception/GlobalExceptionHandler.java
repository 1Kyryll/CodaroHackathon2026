package com.example.hackathoncodaro2026.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied() {
        return "redirect:/";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/register")) {
            redirectAttributes.addFlashAttribute("errorMessage", "That username or email is already registered.");
            return "redirect:/register";
        }
        if (uri != null && uri.startsWith("/admin/users")) {
            redirectAttributes.addFlashAttribute("errorMessage", "That username or email is already registered.");
            return "redirect:/admin/users";
        }
        if (uri != null && uri.startsWith("/profile")) {
            redirectAttributes.addFlashAttribute("errorMessage", "That email is already registered.");
            return "redirect:/profile";
        }
        if (uri != null && uri.contains("/coach-rating")) {
            redirectAttributes.addFlashAttribute("errorMessage", "You already rated this booking.");
            return "redirect:/reservations";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "That change could not be saved.");
        return "redirect:/";
    }

    @ExceptionHandler(ReservationException.class)
    public String handleReservation(ReservationException exception, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/reservations";
    }
}

package com.example.hackathoncodaro2026.controller;

import com.example.hackathoncodaro2026.dto.CoachOfferingRequest;
import com.example.hackathoncodaro2026.exception.ReservationException;
import com.example.hackathoncodaro2026.model.CoachOffering;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.ResourceType;
import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.service.CoachOfferingService;
import com.example.hackathoncodaro2026.service.CoachRatingService;
import com.example.hackathoncodaro2026.service.SportSkillLevelCatalog;
import com.example.hackathoncodaro2026.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CoachOfferingController {

    private final CoachOfferingService coachOfferingService;
    private final UserService userService;
    private final SportSkillLevelCatalog sportSkillLevelCatalog;
    private final CoachRatingService coachRatingService;

    public CoachOfferingController(
            CoachOfferingService coachOfferingService,
            UserService userService,
            SportSkillLevelCatalog sportSkillLevelCatalog,
            CoachRatingService coachRatingService
    ) {
        this.coachOfferingService = coachOfferingService;
        this.userService = userService;
        this.sportSkillLevelCatalog = sportSkillLevelCatalog;
        this.coachRatingService = coachRatingService;
    }

    @GetMapping("/coach/offerings")
    public String list(Authentication authentication, Model model) {
        User coach = requireCoach(authentication);
        if (!model.containsAttribute("coachOfferingRequest")) {
            model.addAttribute("coachOfferingRequest", new CoachOfferingRequest());
        }
        addOfferingModel(model, coach);
        return "coach/offerings";
    }

    @GetMapping("/coach/offerings/{id}/edit")
    public String edit(@PathVariable Long id, Authentication authentication, Model model) {
        User coach = requireCoach(authentication);
        CoachOffering offering = coachOfferingService.findForCoach(coach, id)
                .orElseThrow(() -> new ReservationException("That offering could not be found"));
        CoachOfferingRequest form = new CoachOfferingRequest();
        form.setId(offering.getId());
        form.setSportType(offering.getSportType());
        form.setLevels(offering.getLevels());
        form.setPricePerHour(offering.getPricePerHour());
        model.addAttribute("coachOfferingRequest", form);
        addOfferingModel(model, coach);
        return "coach/offerings";
    }

    @PostMapping("/coach/offerings")
    public String save(
            @Valid @ModelAttribute("coachOfferingRequest") CoachOfferingRequest coachOfferingRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User coach = requireCoach(authentication);
        if (bindingResult.hasErrors()) {
            addOfferingModel(model, coach);
            return "coach/offerings";
        }
        try {
            coachOfferingService.save(coach, coachOfferingRequest);
        } catch (ReservationException ex) {
            if (ex.getField() != null) {
                bindingResult.rejectValue(ex.getField(), "invalid", ex.getMessage());
            } else {
                bindingResult.reject("invalid", ex.getMessage());
            }
            addOfferingModel(model, coach);
            return "coach/offerings";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Offering saved.");
        return "redirect:/coach/offerings";
    }

    @PostMapping("/coach/offerings/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User coach = requireCoach(authentication);
        try {
            coachOfferingService.delete(coach, id);
            redirectAttributes.addFlashAttribute("successMessage", "Offering removed.");
        } catch (ReservationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/coach/offerings";
    }

    private void addOfferingModel(Model model, User coach) {
        model.addAttribute("offerings", coachOfferingService.findForCoach(coach));
        model.addAttribute("sports", ResourceType.values());
        model.addAttribute("sportLevelOptions", sportSkillLevelCatalog.optionsBySport());
        model.addAttribute("sportSkillLevelCatalog", sportSkillLevelCatalog);
        model.addAttribute("coachRatingSummary", coachRatingService.summaryFor(coach.getId()));
    }

    private User requireCoach(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ReservationException("Signed-in user was not found"));
        if (user.getRole() != Role.COACH) {
            throw new ReservationException("Only a coach can edit offerings");
        }
        return user;
    }
}

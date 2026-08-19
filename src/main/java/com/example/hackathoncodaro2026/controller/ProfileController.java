package com.example.hackathoncodaro2026.controller;

import com.example.hackathoncodaro2026.dto.ProfileUpdateRequest;
import com.example.hackathoncodaro2026.exception.DuplicateUserException;
import com.example.hackathoncodaro2026.exception.ReservationException;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.UserSportLevel;
import com.example.hackathoncodaro2026.model.enums.ResourceType;
import com.example.hackathoncodaro2026.repository.UserSportLevelRepository;
import com.example.hackathoncodaro2026.service.AvatarStorageService;
import com.example.hackathoncodaro2026.service.SportSkillLevelCatalog;
import com.example.hackathoncodaro2026.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UserService userService;
    private final UserSportLevelRepository userSportLevelRepository;
    private final SportSkillLevelCatalog sportSkillLevelCatalog;
    private final AvatarStorageService avatarStorageService;

    public ProfileController(
            UserService userService,
            UserSportLevelRepository userSportLevelRepository,
            SportSkillLevelCatalog sportSkillLevelCatalog,
            AvatarStorageService avatarStorageService
    ) {
        this.userService = userService;
        this.userSportLevelRepository = userSportLevelRepository;
        this.sportSkillLevelCatalog = sportSkillLevelCatalog;
        this.avatarStorageService = avatarStorageService;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User user = requireUser(authentication);
        populateProfile(model, user);
        if (!model.containsAttribute("profileUpdateRequest")) {
            ProfileUpdateRequest form = new ProfileUpdateRequest();
            form.setFullName(user.getFullName());
            form.setEmail(user.getEmail());
            form.setPhone(user.getPhone());
            for (UserSportLevel row : userSportLevelRepository.findByUser_Id(user.getId())) {
                form.getSportLevels().put(row.getSportType().name(), row.getSkillLevel());
            }
            model.addAttribute("profileUpdateRequest", form);
        }
        return "profile/edit";
    }

    @PostMapping("/profile")
    public String update(
            @Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest profileUpdateRequest,
            BindingResult bindingResult,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User user = requireUser(authentication);
        populateProfile(model, user);
        if (bindingResult.hasErrors()) {
            return "profile/edit";
        }
        try {
            userService.updateProfile(user, profileUpdateRequest);
            avatarStorageService.store(user, avatar);
        } catch (DuplicateUserException ex) {
            bindingResult.rejectValue(ex.getField(), "invalid", ex.getMessage());
            return "profile/edit";
        } catch (ReservationException ex) {
            if (ex.getField() != null) {
                bindingResult.rejectValue(ex.getField(), "invalid", ex.getMessage());
            } else {
                bindingResult.reject("invalid", ex.getMessage());
            }
            return "profile/edit";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Profile saved.");
        return "redirect:/profile";
    }

    private void populateProfile(Model model, User user) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("profileUserId", user.getId());
        model.addAttribute("sports", ResourceType.values());
        model.addAttribute("sportLevelOptions", sportSkillLevelCatalog.optionsBySport());
    }

    private User requireUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new DuplicateUserException("username", "Signed-in user was not found"));
    }
}

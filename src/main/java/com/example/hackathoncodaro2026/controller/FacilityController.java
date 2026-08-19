package com.example.hackathoncodaro2026.controller;

import com.example.hackathoncodaro2026.model.Facility;
import com.example.hackathoncodaro2026.service.FacilityService;
import com.example.hackathoncodaro2026.service.ResourceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FacilityController {

    private final FacilityService facilityService;
    private final ResourceService resourceService;

    public FacilityController(FacilityService facilityService, ResourceService resourceService) {
        this.facilityService = facilityService;
        this.resourceService = resourceService;
    }

    @GetMapping("/facilities")
    public String list(Model model) {
        model.addAttribute("facilities", facilityService.findAllEnabled());
        return "facilities/list";
    }

    @GetMapping("/facilities/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Facility facility = facilityService.findEnabledById(id).orElse(null);
        if (facility == null) {
            return "redirect:/facilities";
        }
        model.addAttribute("facility", facility);
        model.addAttribute("resources", resourceService.findEnabledByFacility(id));
        return "facilities/detail";
    }
}

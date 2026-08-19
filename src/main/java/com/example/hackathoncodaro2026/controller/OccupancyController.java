package com.example.hackathoncodaro2026.controller;

import com.example.hackathoncodaro2026.dto.OccupancyGrid;
import com.example.hackathoncodaro2026.service.FacilityService;
import com.example.hackathoncodaro2026.service.OccupancyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.ZoneId;

@Controller
public class OccupancyController {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final OccupancyService occupancyService;
    private final FacilityService facilityService;

    public OccupancyController(OccupancyService occupancyService, FacilityService facilityService) {
        this.occupancyService = occupancyService;
        this.facilityService = facilityService;
    }

    @GetMapping("/occupancy")
    public String occupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String facilityId,
            Model model
    ) {
        LocalDate today = LocalDate.now(WARSAW);
        LocalDate selected = date == null ? today : date;
        Long facilityKey = parseFacilityId(facilityId);
        OccupancyGrid grid = occupancyService.gridFor(selected, facilityKey);
        model.addAttribute("grid", grid);
        model.addAttribute("today", today);
        model.addAttribute("selectedDate", selected);
        model.addAttribute("facilityId", facilityKey);
        model.addAttribute("facilities", facilityService.findAllEnabled());
        return "occupancy/index";
    }

    private Long parseFacilityId(String facilityId) {
        if (facilityId == null || facilityId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(facilityId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

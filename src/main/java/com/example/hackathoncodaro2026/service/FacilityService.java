package com.example.hackathoncodaro2026.service;

import com.example.hackathoncodaro2026.model.Facility;

import java.util.List;
import java.util.Optional;

public interface FacilityService {

    List<Facility> findAllEnabled();

    Optional<Facility> findEnabledById(Long id);

    long countEnabled();
}

package com.example.hackathoncodaro2026.service.impl;

import com.example.hackathoncodaro2026.model.Facility;
import com.example.hackathoncodaro2026.repository.FacilityRepository;
import com.example.hackathoncodaro2026.service.FacilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class FacilityServiceImpl implements FacilityService {

    private final FacilityRepository facilityRepository;

    public FacilityServiceImpl(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @Override
    public List<Facility> findAllEnabled() {
        return facilityRepository.findByEnabledTrueOrderByNameAsc();
    }

    @Override
    public Optional<Facility> findEnabledById(Long id) {
        return facilityRepository.findByIdAndEnabledTrue(id);
    }

    @Override
    public long countEnabled() {
        return facilityRepository.countByEnabledTrue();
    }
}

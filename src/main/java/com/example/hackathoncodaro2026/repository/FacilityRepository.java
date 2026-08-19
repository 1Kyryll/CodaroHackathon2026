package com.example.hackathoncodaro2026.repository;

import com.example.hackathoncodaro2026.model.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findByEnabledTrueOrderByNameAsc();

    Optional<Facility> findByIdAndEnabledTrue(Long id);

    long countByEnabledTrue();
}

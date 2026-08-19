package com.example.hackathoncodaro2026.repository;

import com.example.hackathoncodaro2026.model.SportResource;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SportResourceRepository extends JpaRepository<SportResource, Long> {

    List<SportResource> findByFacility_IdAndEnabledTrueOrderByNameAsc(Long facilityId);

    @Query("SELECT r FROM SportResource r JOIN FETCH r.facility WHERE r.id = :id")
    Optional<SportResource> findWithFacilityById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "4000"))
    @Query("SELECT r FROM SportResource r WHERE r.id = :id")
    Optional<SportResource> lockById(@Param("id") Long id);

    @Query("""
            SELECT r FROM SportResource r
            JOIN FETCH r.facility f
            WHERE r.enabled = true AND f.enabled = true
            ORDER BY f.name ASC, r.name ASC
            """)
    List<SportResource> findAllEnabledWithFacility();

    @Query("""
            SELECT r FROM SportResource r
            JOIN FETCH r.facility f
            WHERE r.enabled = true AND f.enabled = true AND f.id = :facilityId
            ORDER BY r.name ASC
            """)
    List<SportResource> findEnabledWithFacilityByFacilityId(@Param("facilityId") Long facilityId);

    long countByEnabledTrue();
}

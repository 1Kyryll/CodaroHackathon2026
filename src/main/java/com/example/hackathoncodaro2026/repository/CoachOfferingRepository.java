package com.example.hackathoncodaro2026.repository;

import com.example.hackathoncodaro2026.model.CoachOffering;
import com.example.hackathoncodaro2026.model.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoachOfferingRepository extends JpaRepository<CoachOffering, Long> {

    List<CoachOffering> findByCoach_IdOrderBySportTypeAsc(Long coachId);

    Optional<CoachOffering> findByIdAndCoach_Id(Long id, Long coachId);

    Optional<CoachOffering> findByCoach_IdAndSportType(Long coachId, ResourceType sportType);

    boolean existsByCoach_IdAndSportType(Long coachId, ResourceType sportType);

    boolean existsByCoach_IdAndSportTypeAndIdNot(Long coachId, ResourceType sportType, Long id);

    @Query("""
            SELECT DISTINCT o FROM CoachOffering o
            JOIN FETCH o.coach c
            WHERE c.role = com.example.hackathoncodaro2026.model.enums.Role.COACH
              AND c.enabled = true
            ORDER BY c.fullName ASC, o.sportType ASC
            """)
    List<CoachOffering> findPublished();

    @Query("""
            SELECT DISTINCT o FROM CoachOffering o
            JOIN FETCH o.coach c
            JOIN o.levels level
            WHERE c.role = com.example.hackathoncodaro2026.model.enums.Role.COACH
              AND c.enabled = true
              AND o.sportType = :sport
              AND level = :level
            ORDER BY c.fullName ASC
            """)
    List<CoachOffering> findPublishedCovering(
            @Param("sport") ResourceType sport,
            @Param("level") String level
    );
}

package com.example.hackathoncodaro2026.repository;

import com.example.hackathoncodaro2026.model.CoachRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CoachRatingRepository extends JpaRepository<CoachRating, Long> {

    Optional<CoachRating> findByReservation_Id(Long reservationId);

    boolean existsByReservation_Id(Long reservationId);

    List<CoachRating> findByReservation_IdIn(Collection<Long> reservationIds);

    @Query("""
            SELECT r.coach.id, AVG(r.stars), COUNT(r.id)
            FROM CoachRating r
            WHERE r.coach.id IN :coachIds
            GROUP BY r.coach.id
            """)
    List<Object[]> aggregateForCoachIds(@Param("coachIds") Collection<Long> coachIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM CoachRating r
            WHERE r.reservation.id IN (
                SELECT res.id FROM Reservation res WHERE res.endAt < :cutoff
            )
            """)
    int deleteForReservationsEndedBefore(@Param("cutoff") LocalDateTime cutoff);
}

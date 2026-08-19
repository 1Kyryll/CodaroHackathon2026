package com.example.hackathoncodaro2026.repository;

import com.example.hackathoncodaro2026.model.ReservationExtra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ReservationExtraRepository extends JpaRepository<ReservationExtra, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM ReservationExtra e
            WHERE e.reservation.id IN (
                SELECT r.id FROM Reservation r WHERE r.endAt < :cutoff
            )
            """)
    int deleteForReservationsEndedBefore(@Param("cutoff") LocalDateTime cutoff);
}

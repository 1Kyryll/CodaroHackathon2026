package com.example.hackathoncodaro2026.service;

import com.example.hackathoncodaro2026.dto.ReservationRequest;
import com.example.hackathoncodaro2026.dto.ReservationUpdateResult;
import com.example.hackathoncodaro2026.model.Reservation;
import com.example.hackathoncodaro2026.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationService {

    Reservation create(User user, ReservationRequest request);

    ReservationUpdateResult update(User actor, Long reservationId, ReservationRequest request);

    void cancel(User actor, Long reservationId);

    void cancel(User actor, Long reservationId, String reason);

    void cancel(User actor, Long reservationId, String reason, String otherNote);

    void confirm(User actor, Long reservationId);

    List<Reservation> findForUser(User user);

    List<Reservation> findAll();

    List<Reservation> findManagerQueue(LocalDate date);

    List<Reservation> findForCoach(User coach);

    long countUpcomingActive(User user);

    long countUpcomingAsCoach(User coach);

    int deleteEndedBefore(LocalDateTime cutoff);

    int deleteEndedOlderThanOneMonth();

    Optional<Reservation> findWithDetails(Long id);

    boolean canEdit(User actor, Reservation reservation);
}

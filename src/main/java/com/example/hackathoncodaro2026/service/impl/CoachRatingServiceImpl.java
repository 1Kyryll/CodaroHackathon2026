package com.example.hackathoncodaro2026.service.impl;

import com.example.hackathoncodaro2026.dto.CoachRatingRequest;
import com.example.hackathoncodaro2026.dto.CoachRatingSummary;
import com.example.hackathoncodaro2026.exception.ReservationException;
import com.example.hackathoncodaro2026.model.CoachRating;
import com.example.hackathoncodaro2026.model.Reservation;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.ReservationStatus;
import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.repository.CoachRatingRepository;
import com.example.hackathoncodaro2026.repository.ReservationRepository;
import com.example.hackathoncodaro2026.service.CoachRatingService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class CoachRatingServiceImpl implements CoachRatingService {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final CoachRatingRepository coachRatingRepository;
    private final ReservationRepository reservationRepository;

    public CoachRatingServiceImpl(
            CoachRatingRepository coachRatingRepository,
            ReservationRepository reservationRepository
    ) {
        this.coachRatingRepository = coachRatingRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional
    public CoachRating rate(User author, Long reservationId, CoachRatingRequest request) {
        Reservation reservation = reservationRepository.findWithDetailsById(reservationId)
                .orElseThrow(() -> new ReservationException("That reservation could not be found"));
        if (author == null || reservation.getUser() == null
                || !reservation.getUser().getId().equals(author.getId())) {
            throw new ReservationException("You can only rate your own booking");
        }
        if (reservation.getCoach() == null || reservation.getCoach().getRole() != Role.COACH) {
            throw new ReservationException("A coach must be assigned to this booking");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ReservationException("Only a completed confirmed booking can be rated");
        }
        if (!reservation.getEndAt().isBefore(LocalDateTime.now(WARSAW))) {
            throw new ReservationException("This session has not ended yet");
        }
        if (coachRatingRepository.existsByReservation_Id(reservation.getId())) {
            throw new ReservationException("You already rated this booking");
        }
        if (request == null || request.getStars() == null || request.getStars() < 1 || request.getStars() > 5) {
            throw new ReservationException("Choose a rating from 1 to 5 stars");
        }
        String review = request.getReview() == null ? null : request.getReview().trim();
        if (review != null && review.isEmpty()) {
            review = null;
        }
        if (review != null && review.length() > 500) {
            throw new ReservationException("Review must be 500 characters or fewer");
        }
        CoachRating rating = new CoachRating();
        rating.setCoach(reservation.getCoach());
        rating.setAuthor(author);
        rating.setReservation(reservation);
        rating.setStars(request.getStars());
        rating.setReview(review);
        try {
            return coachRatingRepository.saveAndFlush(rating);
        } catch (DataIntegrityViolationException ex) {
            throw new ReservationException("You already rated this booking");
        }
    }

    @Override
    public boolean canRate(User actor, Reservation reservation) {
        if (actor == null || reservation == null || reservation.getId() == null) {
            return false;
        }
        if (reservation.getUser() == null || !reservation.getUser().getId().equals(actor.getId())) {
            return false;
        }
        if (reservation.getCoach() == null || reservation.getCoach().getRole() != Role.COACH) {
            return false;
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            return false;
        }
        if (!reservation.getEndAt().isBefore(LocalDateTime.now(WARSAW))) {
            return false;
        }
        return !coachRatingRepository.existsByReservation_Id(reservation.getId());
    }

    @Override
    public Map<Long, CoachRating> findByReservationIds(Collection<Long> reservationIds) {
        Map<Long, CoachRating> result = new HashMap<>();
        if (reservationIds == null || reservationIds.isEmpty()) {
            return result;
        }
        List<Long> ids = reservationIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return result;
        }
        for (CoachRating rating : coachRatingRepository.findByReservation_IdIn(ids)) {
            result.put(rating.getReservation().getId(), rating);
        }
        return result;
    }

    @Override
    public Map<Long, CoachRatingSummary> summariesFor(Collection<Long> coachIds) {
        Map<Long, CoachRatingSummary> result = new LinkedHashMap<>();
        if (coachIds == null || coachIds.isEmpty()) {
            return result;
        }
        List<Long> ids = coachIds.stream().filter(Objects::nonNull).distinct().toList();
        for (Long id : ids) {
            result.put(id, CoachRatingSummary.empty());
        }
        if (ids.isEmpty()) {
            return result;
        }
        for (Object[] row : coachRatingRepository.aggregateForCoachIds(ids)) {
            if (row == null || row.length < 3 || row[0] == null) {
                continue;
            }
            Long id = ((Number) row[0]).longValue();
            double average = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            long count = row[2] == null ? 0L : ((Number) row[2]).longValue();
            result.put(id, new CoachRatingSummary(average, count));
        }
        return result;
    }

    @Override
    public CoachRatingSummary summaryFor(Long coachId) {
        if (coachId == null) {
            return CoachRatingSummary.empty();
        }
        return summariesFor(List.of(coachId)).getOrDefault(coachId, CoachRatingSummary.empty());
    }
}

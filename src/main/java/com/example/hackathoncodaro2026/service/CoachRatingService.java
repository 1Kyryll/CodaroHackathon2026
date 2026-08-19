package com.example.hackathoncodaro2026.service;

import com.example.hackathoncodaro2026.dto.CoachRatingRequest;
import com.example.hackathoncodaro2026.dto.CoachRatingSummary;
import com.example.hackathoncodaro2026.model.CoachRating;
import com.example.hackathoncodaro2026.model.Reservation;
import com.example.hackathoncodaro2026.model.User;

import java.util.Collection;
import java.util.Map;

public interface CoachRatingService {

    CoachRating rate(User author, Long reservationId, CoachRatingRequest request);

    boolean canRate(User actor, Reservation reservation);

    Map<Long, CoachRating> findByReservationIds(Collection<Long> reservationIds);

    Map<Long, CoachRatingSummary> summariesFor(Collection<Long> coachIds);

    CoachRatingSummary summaryFor(Long coachId);
}

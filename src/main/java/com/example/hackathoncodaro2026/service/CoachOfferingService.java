package com.example.hackathoncodaro2026.service;

import com.example.hackathoncodaro2026.dto.CoachOfferingRequest;
import com.example.hackathoncodaro2026.dto.CoachPickerCard;
import com.example.hackathoncodaro2026.model.CoachOffering;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.ResourceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CoachOfferingService {

    List<CoachOffering> findForCoach(User coach);

    Optional<CoachOffering> findForCoach(User coach, Long offeringId);

    CoachOffering save(User coach, CoachOfferingRequest request);

    void delete(User coach, Long offeringId);

    List<CoachOffering> findPublished();

    List<CoachOffering> findPublished(ResourceType sport, String level);

    Optional<CoachOffering> findByCoachAndSport(Long coachId, ResourceType sport);

    List<CoachPickerCard> pickerCards(ResourceType sport);

    BigDecimal feeFor(Long coachId, ResourceType sport, int hours);
}

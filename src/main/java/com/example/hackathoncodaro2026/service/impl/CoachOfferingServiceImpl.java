package com.example.hackathoncodaro2026.service.impl;

import com.example.hackathoncodaro2026.dto.CoachOfferingRequest;
import com.example.hackathoncodaro2026.dto.CoachPickerCard;
import com.example.hackathoncodaro2026.dto.CoachRatingSummary;
import com.example.hackathoncodaro2026.exception.ReservationException;
import com.example.hackathoncodaro2026.model.CoachOffering;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.ResourceType;
import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.repository.CoachOfferingRepository;
import com.example.hackathoncodaro2026.service.AuditLogService;
import com.example.hackathoncodaro2026.service.CoachOfferingService;
import com.example.hackathoncodaro2026.service.CoachRatingService;
import com.example.hackathoncodaro2026.service.SportSkillLevelCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CoachOfferingServiceImpl implements CoachOfferingService {

    private final CoachOfferingRepository coachOfferingRepository;
    private final SportSkillLevelCatalog sportSkillLevelCatalog;
    private final CoachRatingService coachRatingService;
    private final AuditLogService auditLogService;

    public CoachOfferingServiceImpl(
            CoachOfferingRepository coachOfferingRepository,
            SportSkillLevelCatalog sportSkillLevelCatalog,
            CoachRatingService coachRatingService,
            AuditLogService auditLogService
    ) {
        this.coachOfferingRepository = coachOfferingRepository;
        this.sportSkillLevelCatalog = sportSkillLevelCatalog;
        this.coachRatingService = coachRatingService;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<CoachOffering> findForCoach(User coach) {
        return coachOfferingRepository.findByCoach_IdOrderBySportTypeAsc(coach.getId());
    }

    @Override
    public Optional<CoachOffering> findForCoach(User coach, Long offeringId) {
        return coachOfferingRepository.findByIdAndCoach_Id(offeringId, coach.getId());
    }

    @Override
    @Transactional
    public CoachOffering save(User coach, CoachOfferingRequest request) {
        if (coach.getRole() != Role.COACH) {
            throw new ReservationException("Only a coach can edit offerings");
        }
        if (request.getSportType() == null) {
            throw new ReservationException("sportType", "Choose a sport");
        }
        Set<String> levels = request.getLevels() == null ? Set.of() : new LinkedHashSet<>(request.getLevels());
        levels.remove(null);
        levels.removeIf(level -> level == null || level.isBlank());
        if (levels.isEmpty()) {
            throw new ReservationException("levels", "Choose at least one level");
        }
        Set<String> allowed = sportSkillLevelCatalog.codesFor(request.getSportType());
        if (!allowed.containsAll(levels)) {
            throw new ReservationException("levels", "Choose levels that belong to this sport");
        }
        if (request.getPricePerHour() == null || request.getPricePerHour().signum() <= 0) {
            throw new ReservationException("pricePerHour", "Enter an hourly price greater than zero");
        }
        CoachOffering offering;
        if (request.getId() != null) {
            offering = coachOfferingRepository.findByIdAndCoach_Id(request.getId(), coach.getId())
                    .orElseThrow(() -> new ReservationException("That offering could not be found"));
            if (coachOfferingRepository.existsByCoach_IdAndSportTypeAndIdNot(
                    coach.getId(),
                    request.getSportType(),
                    request.getId()
            )) {
                throw new ReservationException("sportType", "You already have an offering for this sport");
            }
        } else {
            if (coachOfferingRepository.existsByCoach_IdAndSportType(coach.getId(), request.getSportType())) {
                throw new ReservationException("sportType", "You already have an offering for this sport");
            }
            offering = new CoachOffering();
            offering.setCoach(coach);
        }
        offering.setSportType(request.getSportType());
        offering.setLevels(new LinkedHashSet<>(levels));
        offering.setPricePerHour(request.getPricePerHour().setScale(2, RoundingMode.HALF_UP));
        CoachOffering saved = coachOfferingRepository.save(offering);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("sport", saved.getSportType());
        details.put("levels", String.join(",", saved.getLevels()));
        details.put("pricePerHour", saved.getPricePerHour());
        auditLogService.record(
                coach,
                request.getId() == null ? "OFFERING_CREATE" : "OFFERING_UPDATE",
                "COACH_OFFERING",
                saved.getId(),
                "SUCCESS",
                details
        );
        return saved;
    }

    @Override
    @Transactional
    public void delete(User coach, Long offeringId) {
        CoachOffering offering = coachOfferingRepository.findByIdAndCoach_Id(offeringId, coach.getId())
                .orElseThrow(() -> new ReservationException("That offering could not be found"));
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("sport", offering.getSportType());
        details.put("levels", offering.getLevels() == null ? "" : String.join(",", offering.getLevels()));
        details.put("pricePerHour", offering.getPricePerHour());
        Long id = offering.getId();
        coachOfferingRepository.delete(offering);
        auditLogService.record(coach, "OFFERING_DELETE", "COACH_OFFERING", id, "SUCCESS", details);
    }

    @Override
    public List<CoachOffering> findPublished() {
        return coachOfferingRepository.findPublished();
    }

    @Override
    public List<CoachOffering> findPublished(ResourceType sport, String level) {
        if (sport == null || level == null || level.isBlank()) {
            return findPublished();
        }
        return coachOfferingRepository.findPublishedCovering(sport, level.trim());
    }

    @Override
    public Optional<CoachOffering> findByCoachAndSport(Long coachId, ResourceType sport) {
        return coachOfferingRepository.findByCoach_IdAndSportType(coachId, sport);
    }

    @Override
    public List<CoachPickerCard> pickerCards(ResourceType sport) {
        if (sport == null) {
            return List.of();
        }
        List<CoachOffering> offerings = new ArrayList<>();
        for (CoachOffering offering : coachOfferingRepository.findPublished()) {
            if (offering.getSportType() == sport) {
                offerings.add(offering);
            }
        }
        Map<Long, CoachRatingSummary> summaries = coachRatingService.summariesFor(
                offerings.stream().map(item -> item.getCoach().getId()).toList()
        );
        List<CoachPickerCard> cards = new ArrayList<>();
        for (CoachOffering offering : offerings) {
            User coach = offering.getCoach();
            CoachRatingSummary summary = summaries.getOrDefault(coach.getId(), CoachRatingSummary.empty());
            CoachPickerCard card = new CoachPickerCard();
            card.setId(coach.getId());
            card.setFullName(coach.getFullName());
            card.setInitials(coach.getInitials());
            card.setPricePerHour(offering.getPricePerHour());
            card.setLevels(new LinkedHashSet<>(offering.getLevels()));
            card.setLevelsLabel(sportSkillLevelCatalog.joinedLabels(sport, offering.getLevels()));
            card.setAverageRating(summary.getAverage());
            card.setRatingCount(summary.getCount());
            card.setRatingLabel(summary.getDisplayLabel());
            cards.add(card);
        }
        return cards;
    }

    @Override
    public BigDecimal feeFor(Long coachId, ResourceType sport, int hours) {
        if (coachId == null || sport == null || hours < 1) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        CoachOffering offering = coachOfferingRepository.findByCoach_IdAndSportType(coachId, sport).orElse(null);
        if (offering == null || offering.getPricePerHour() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return offering.getPricePerHour()
                .multiply(BigDecimal.valueOf(hours))
                .setScale(2, RoundingMode.HALF_UP);
    }
}

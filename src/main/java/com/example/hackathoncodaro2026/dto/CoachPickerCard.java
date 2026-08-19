package com.example.hackathoncodaro2026.dto;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CoachPickerCard {

    private Long id;
    private String fullName;
    private String initials;
    private BigDecimal pricePerHour;
    private Set<String> levels = new LinkedHashSet<>();
    private String levelsLabel;
    private double averageRating;
    private long ratingCount;
    private String ratingLabel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public Set<String> getLevels() {
        return levels;
    }

    public void setLevels(Set<String> levels) {
        this.levels = levels;
    }

    public String getLevelsLabel() {
        return levelsLabel;
    }

    public void setLevelsLabel(String levelsLabel) {
        this.levelsLabel = levelsLabel;
    }

    public String getLevelsCsv() {
        if (levels == null || levels.isEmpty()) {
            return "";
        }
        return levels.stream().collect(Collectors.joining(","));
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public long getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(long ratingCount) {
        this.ratingCount = ratingCount;
    }

    public String getRatingLabel() {
        return ratingLabel;
    }

    public void setRatingLabel(String ratingLabel) {
        this.ratingLabel = ratingLabel;
    }
}

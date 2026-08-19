package com.example.hackathoncodaro2026.dto;

import com.example.hackathoncodaro2026.model.enums.ResourceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public class CoachOfferingRequest {

    private Long id;

    @NotNull
    private ResourceType sportType;

    @NotEmpty
    private Set<String> levels = new LinkedHashSet<>();

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal pricePerHour;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ResourceType getSportType() {
        return sportType;
    }

    public void setSportType(ResourceType sportType) {
        this.sportType = sportType;
    }

    public Set<String> getLevels() {
        return levels;
    }

    public void setLevels(Set<String> levels) {
        this.levels = levels;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }
}

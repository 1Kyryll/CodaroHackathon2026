package com.example.hackathoncodaro2026.model;

import com.example.hackathoncodaro2026.model.enums.ResourceType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(
        name = "coach_offerings",
        uniqueConstraints = @UniqueConstraint(name = "uk_coach_offering_sport", columnNames = {"coach_id", "sport_type"})
)
public class CoachOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id", nullable = false)
    private User coach;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "sport_type", nullable = false, length = 32)
    private ResourceType sportType;

    @NotEmpty
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "coach_offering_levels", joinColumns = @JoinColumn(name = "offering_id"))
    @Column(name = "skill_level", nullable = false, length = 32)
    private Set<String> levels = new LinkedHashSet<>();

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "price_per_hour", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour;

    public CoachOffering() {
    }

    public boolean covers(String level) {
        return level != null && levels.contains(level);
    }

    public String getLevelsLabel() {
        return levels.stream().collect(Collectors.joining(", "));
    }

    public String getFormattedPrice() {
        BigDecimal amount = pricePerHour == null ? BigDecimal.ZERO : pricePerHour;
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString() + " PLN / h";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCoach() {
        return coach;
    }

    public void setCoach(User coach) {
        this.coach = coach;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CoachOffering that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

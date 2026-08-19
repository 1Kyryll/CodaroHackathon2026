package com.example.hackathoncodaro2026.model;

import com.example.hackathoncodaro2026.model.enums.ReservationKind;
import com.example.hackathoncodaro2026.model.enums.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "resources")
public class SportResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceType type;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Valid
    @NotNull
    @Embedded
    private Address address;

    @Min(1)
    @Column(nullable = false)
    private int capacity = 1;

    @Min(1)
    @ColumnDefault("1")
    @Column(name = "min_party_size", nullable = false)
    private int minPartySize = 1;

    @Min(1)
    @ColumnDefault("1")
    @Column(name = "max_party_size", nullable = false)
    private int maxPartySize = 1;

    @Min(1)
    @ColumnDefault("1")
    @Column(name = "lesson_min_party_size", nullable = false)
    private int lessonMinPartySize = 1;

    @Min(1)
    @ColumnDefault("1")
    @Column(name = "lesson_max_party_size", nullable = false)
    private int lessonMaxPartySize = 1;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @ColumnDefault("0")
    @Column(name = "base_hourly_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseHourlyPrice = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @ColumnDefault("0")
    @Column(name = "lesson_hourly_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal lessonHourlyPrice = BigDecimal.ZERO;

    @Min(15)
    @Column(name = "slot_duration_minutes", nullable = false)
    private int slotDurationMinutes = 60;

    @NotNull
    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    @NotNull
    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    @Column(nullable = false)
    private boolean enabled = true;

    @Size(max = 160)
    @Column(name = "image_path", length = 160)
    private String imagePath;

    public SportResource() {
    }

    @AssertTrue
    public boolean isClosingAfterOpening() {
        if (openingTime == null || closingTime == null) {
            return true;
        }
        return closingTime.isAfter(openingTime);
    }

    @AssertTrue
    public boolean isPartySizeRangeValid() {
        return maxPartySize >= minPartySize;
    }

    @AssertTrue
    public boolean isLessonPartySizeRangeValid() {
        return lessonMaxPartySize >= lessonMinPartySize;
    }

    public boolean requiresPartySize() {
        return maxPartySize > 1;
    }

    public boolean requiresLessonPartySize() {
        return requiresBookingMode() && capacity > 1;
    }

    public boolean requiresBookingMode() {
        return minPartySize == 1 && maxPartySize == 1;
    }

    public boolean requiresAttendeeCount(ReservationKind kind) {
        if (kind == ReservationKind.LESSON) {
            return requiresLessonPartySize();
        }
        return requiresPartySize();
    }

    public int attendeeMin(ReservationKind kind) {
        if (kind == ReservationKind.LESSON && requiresLessonPartySize()) {
            return 2;
        }
        return minPartySize;
    }

    public int attendeeMax(ReservationKind kind) {
        if (kind == ReservationKind.LESSON && requiresLessonPartySize()) {
            return capacity;
        }
        return maxPartySize;
    }

    public String partySizeLabel(int size) {
        return partySizeLabel(size, null);
    }

    public String partySizeLabel(int size, ReservationKind kind) {
        if (kind != ReservationKind.LESSON && requiresPartySize() && size == maxPartySize) {
            return maxPartySize + "+";
        }
        return String.valueOf(size);
    }

    public BigDecimal effectiveBaseHourlyPrice() {
        if (baseHourlyPrice != null && baseHourlyPrice.compareTo(BigDecimal.ZERO) > 0) {
            return baseHourlyPrice;
        }
        if (type != null && type.getBaseHourlyPrice() != null) {
            return type.getBaseHourlyPrice();
        }
        return BigDecimal.ZERO.setScale(2);
    }

    public BigDecimal effectiveLessonHourlyPrice() {
        if (lessonHourlyPrice != null && lessonHourlyPrice.compareTo(BigDecimal.ZERO) > 0) {
            return lessonHourlyPrice;
        }
        if (type != null && type.getLessonHourlyPrice() != null && type.getLessonHourlyPrice().compareTo(BigDecimal.ZERO) > 0) {
            return type.getLessonHourlyPrice();
        }
        return effectiveBaseHourlyPrice().multiply(new BigDecimal("3")).setScale(2);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getMinPartySize() {
        return minPartySize;
    }

    public void setMinPartySize(int minPartySize) {
        this.minPartySize = minPartySize;
    }

    public int getMaxPartySize() {
        return maxPartySize;
    }

    public void setMaxPartySize(int maxPartySize) {
        this.maxPartySize = maxPartySize;
    }

    public int getLessonMinPartySize() {
        return lessonMinPartySize;
    }

    public void setLessonMinPartySize(int lessonMinPartySize) {
        this.lessonMinPartySize = lessonMinPartySize;
    }

    public int getLessonMaxPartySize() {
        return lessonMaxPartySize;
    }

    public void setLessonMaxPartySize(int lessonMaxPartySize) {
        this.lessonMaxPartySize = lessonMaxPartySize;
    }

    public BigDecimal getBaseHourlyPrice() {
        return baseHourlyPrice;
    }

    public void setBaseHourlyPrice(BigDecimal baseHourlyPrice) {
        this.baseHourlyPrice = baseHourlyPrice;
    }

    public BigDecimal getLessonHourlyPrice() {
        return lessonHourlyPrice;
    }

    public void setLessonHourlyPrice(BigDecimal lessonHourlyPrice) {
        this.lessonHourlyPrice = lessonHourlyPrice;
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(int slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(LocalTime openingTime) {
        this.openingTime = openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(LocalTime closingTime) {
        this.closingTime = closingTime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SportResource that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

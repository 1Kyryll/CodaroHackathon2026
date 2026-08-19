package com.example.hackathoncodaro2026.dto;

import com.example.hackathoncodaro2026.model.enums.PaymentMethod;
import com.example.hackathoncodaro2026.model.enums.ReservationKind;
import com.example.hackathoncodaro2026.validation.ReservationTimeValid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@ReservationTimeValid
public class ReservationRequest {

    @NotNull
    private Long resourceId;

    @NotNull
    @FutureOrPresent
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    @NotNull
    @Min(1)
    @Max(4)
    private Integer durationHours = 1;

    @Size(max = 300)
    private String note;

    @Pattern(regexp = "^$|^[+]?[0-9\\s().-]{7,20}$")
    @Size(max = 20)
    private String phone;

    @Min(1)
    private Integer partySize;

    @NotNull
    private PaymentMethod paymentMethod;

    private ReservationKind kind;

    private List<Long> extraIds = new ArrayList<>();

    private Long coachId;

    @Size(max = 32)
    private String skillLevel;

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getPartySize() {
        return partySize;
    }

    public void setPartySize(Integer partySize) {
        this.partySize = partySize;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public ReservationKind getKind() {
        return kind;
    }

    public void setKind(ReservationKind kind) {
        this.kind = kind;
    }

    public List<Long> getExtraIds() {
        return extraIds;
    }

    public void setExtraIds(List<Long> extraIds) {
        this.extraIds = extraIds;
    }

    public Long getCoachId() {
        return coachId;
    }

    public void setCoachId(Long coachId) {
        this.coachId = (coachId == null || coachId <= 0) ? null : coachId;
    }

    public String getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(String skillLevel) {
        this.skillLevel = skillLevel;
    }
}

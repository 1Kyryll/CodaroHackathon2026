package com.example.hackathoncodaro2026.dto;

import com.example.hackathoncodaro2026.model.Reservation;

import java.math.BigDecimal;

public record ReservationUpdateResult(
        Reservation reservation,
        BigDecimal previousAmount,
        BigDecimal newAmount
) {
    public boolean amountChanged() {
        if (previousAmount == null || newAmount == null) {
            return previousAmount != newAmount;
        }
        return previousAmount.compareTo(newAmount) != 0;
    }
}

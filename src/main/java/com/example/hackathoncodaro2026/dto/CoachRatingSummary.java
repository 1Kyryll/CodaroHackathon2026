package com.example.hackathoncodaro2026.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CoachRatingSummary {

    private final double average;
    private final long count;

    public CoachRatingSummary(double average, long count) {
        this.average = average;
        this.count = count;
    }

    public static CoachRatingSummary empty() {
        return new CoachRatingSummary(0.0, 0L);
    }

    public double getAverage() {
        return average;
    }

    public long getCount() {
        return count;
    }

    public boolean hasRatings() {
        return count > 0;
    }

    public String getDisplayLabel() {
        if (count < 1) {
            return "New coach";
        }
        String avg = BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP).toPlainString();
        return "★ " + avg + " (" + count + ")";
    }
}

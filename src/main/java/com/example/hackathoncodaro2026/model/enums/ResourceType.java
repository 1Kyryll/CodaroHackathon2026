package com.example.hackathoncodaro2026.model.enums;

import java.math.BigDecimal;

public enum ResourceType {
    TENNIS("Tennis", "/images/sports/tennis.jpg", 2, 4, 1, 1, new BigDecimal("80.00"), BigDecimal.ZERO),
    FOOTBALL("Football", "/images/sports/football.jpg", 2, 22, 1, 1, new BigDecimal("160.00"), BigDecimal.ZERO),
    BASKETBALL("Basketball", "/images/sports/basketball.jpg", 2, 10, 1, 1, new BigDecimal("120.00"), BigDecimal.ZERO),
    VOLLEYBALL("Volleyball", "/images/sports/volleyball.jpg", 2, 12, 1, 1, new BigDecimal("110.00"), BigDecimal.ZERO),
    SWIMMING("Swimming", "/images/sports/swimming.jpg", 1, 1, 2, 8, new BigDecimal("35.00"), new BigDecimal("95.00")),
    GYM("Gym", "/images/sports/gym.jpg", 1, 1, 2, 12, new BigDecimal("30.00"), new BigDecimal("90.00")),
    SQUASH("Squash", "/images/sports/squash.jpg", 2, 4, 1, 1, new BigDecimal("70.00"), BigDecimal.ZERO);

    private final String displayName;
    private final String imagePath;
    private final int minPartySize;
    private final int maxPartySize;
    private final int lessonMinPartySize;
    private final int lessonMaxPartySize;
    private final BigDecimal baseHourlyPrice;
    private final BigDecimal lessonHourlyPrice;

    ResourceType(
            String displayName,
            String imagePath,
            int minPartySize,
            int maxPartySize,
            int lessonMinPartySize,
            int lessonMaxPartySize,
            BigDecimal baseHourlyPrice,
            BigDecimal lessonHourlyPrice
    ) {
        this.displayName = displayName;
        this.imagePath = imagePath;
        this.minPartySize = minPartySize;
        this.maxPartySize = maxPartySize;
        this.lessonMinPartySize = lessonMinPartySize;
        this.lessonMaxPartySize = lessonMaxPartySize;
        this.baseHourlyPrice = baseHourlyPrice;
        this.lessonHourlyPrice = lessonHourlyPrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int getMinPartySize() {
        return minPartySize;
    }

    public int getMaxPartySize() {
        return maxPartySize;
    }

    public int getLessonMinPartySize() {
        return lessonMinPartySize;
    }

    public int getLessonMaxPartySize() {
        return lessonMaxPartySize;
    }

    public BigDecimal getBaseHourlyPrice() {
        return baseHourlyPrice;
    }

    public BigDecimal getLessonHourlyPrice() {
        return lessonHourlyPrice;
    }
}

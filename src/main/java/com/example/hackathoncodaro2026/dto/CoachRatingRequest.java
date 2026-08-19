package com.example.hackathoncodaro2026.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CoachRatingRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer stars;

    @Size(max = 500)
    private String review;

    public Integer getStars() {
        return stars;
    }

    public void setStars(Integer stars) {
        this.stars = stars;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }
}

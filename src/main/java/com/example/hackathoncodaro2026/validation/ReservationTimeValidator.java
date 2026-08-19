package com.example.hackathoncodaro2026.validation;

import com.example.hackathoncodaro2026.dto.ReservationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class ReservationTimeValidator implements ConstraintValidator<ReservationTimeValid, ReservationRequest> {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Override
    public boolean isValid(ReservationRequest value, ConstraintValidatorContext context) {
        if (value == null || value.getDate() == null || value.getStartTime() == null) {
            return true;
        }
        LocalDateTime startAt = LocalDateTime.of(value.getDate(), value.getStartTime());
        LocalDateTime now = LocalDateTime.now(WARSAW);
        boolean future = startAt.isAfter(now);
        if (!future) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("startTime")
                    .addConstraintViolation();
        }
        return future;
    }
}

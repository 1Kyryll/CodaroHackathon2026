package com.example.hackathoncodaro2026.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ProfilePasswordChangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProfilePasswordChange {

    String message() default "{ProfilePasswordChange.profileUpdateRequest}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

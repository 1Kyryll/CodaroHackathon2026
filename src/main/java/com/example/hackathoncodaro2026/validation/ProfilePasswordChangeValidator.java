package com.example.hackathoncodaro2026.validation;

import com.example.hackathoncodaro2026.dto.ProfileUpdateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProfilePasswordChangeValidator implements ConstraintValidator<ProfilePasswordChange, ProfileUpdateRequest> {

    @Override
    public boolean isValid(ProfileUpdateRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        boolean currentBlank = isBlank(value.getCurrentPassword());
        boolean newBlank = isBlank(value.getNewPassword());
        boolean confirmBlank = isBlank(value.getConfirmPassword());
        if (currentBlank && newBlank && confirmBlank) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        boolean valid = true;
        if (currentBlank) {
            context.buildConstraintViolationWithTemplate("Enter your current password to change it")
                    .addPropertyNode("currentPassword")
                    .addConstraintViolation();
            valid = false;
        }
        if (newBlank || value.getNewPassword().length() < 8) {
            context.buildConstraintViolationWithTemplate("New password must be at least 8 characters")
                    .addPropertyNode("newPassword")
                    .addConstraintViolation();
            valid = false;
        }
        if (confirmBlank || (value.getNewPassword() != null && !value.getNewPassword().equals(value.getConfirmPassword()))) {
            context.buildConstraintViolationWithTemplate("Passwords do not match")
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
            valid = false;
        }
        return valid;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

package com.example.hackathoncodaro2026.dto;

import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.validation.PasswordConfirmable;
import com.example.hackathoncodaro2026.validation.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordMatches
public class AdminUserCreateRequest implements PasswordConfirmable {

    @NotBlank
    @Size(min = 3, max = 32)
    @Pattern(regexp = "^[A-Za-z0-9_]+$")
    private String username;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(min = 2, max = 100)
    private String fullName;

    @Pattern(regexp = "^$|^[+]?[0-9\\s().-]{7,20}$")
    @Size(max = 20)
    private String phone;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotNull
    private Role role = Role.MANAGER;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}

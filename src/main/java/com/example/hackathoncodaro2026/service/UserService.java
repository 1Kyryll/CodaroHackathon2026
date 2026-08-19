package com.example.hackathoncodaro2026.service;

import com.example.hackathoncodaro2026.dto.AdminUserCreateRequest;
import com.example.hackathoncodaro2026.dto.ProfileUpdateRequest;
import com.example.hackathoncodaro2026.dto.RegistrationRequest;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.ResourceType;

import java.util.Optional;

public interface UserService {

    User register(RegistrationRequest request);

    User updateProfile(User user, ProfileUpdateRequest request);

    User updatePhone(User user, String phone);

    User createStaff(AdminUserCreateRequest request);

    void saveSportLevel(User user, ResourceType sport, String level);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}

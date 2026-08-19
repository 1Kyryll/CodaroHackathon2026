package com.example.hackathoncodaro2026.service.impl;

import com.example.hackathoncodaro2026.dto.AdminUserCreateRequest;
import com.example.hackathoncodaro2026.dto.ProfileUpdateRequest;
import com.example.hackathoncodaro2026.dto.RegistrationRequest;
import com.example.hackathoncodaro2026.exception.DuplicateUserException;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.UserSportLevel;
import com.example.hackathoncodaro2026.model.enums.ResourceType;
import com.example.hackathoncodaro2026.model.enums.Role;
import com.example.hackathoncodaro2026.repository.UserRepository;
import com.example.hackathoncodaro2026.repository.UserSportLevelRepository;
import com.example.hackathoncodaro2026.service.SportSkillLevelCatalog;
import com.example.hackathoncodaro2026.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserSportLevelRepository userSportLevelRepository;
    private final PasswordEncoder passwordEncoder;
    private final SportSkillLevelCatalog sportSkillLevelCatalog;

    public UserServiceImpl(
            UserRepository userRepository,
            UserSportLevelRepository userSportLevelRepository,
            PasswordEncoder passwordEncoder,
            SportSkillLevelCatalog sportSkillLevelCatalog
    ) {
        this.userRepository = userRepository;
        this.userSportLevelRepository = userSportLevelRepository;
        this.passwordEncoder = passwordEncoder;
        this.sportSkillLevelCatalog = sportSkillLevelCatalog;
    }

    @Override
    @Transactional
    public User register(RegistrationRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateUserException("username", "This username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException("email", "This email is already registered");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        user.setRole(Role.USER);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateProfile(User user, ProfileUpdateRequest request) {
        User managed = userRepository.findById(user.getId())
                .orElseThrow(() -> new DuplicateUserException("username", "Signed-in user was not found"));
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, managed.getId())) {
            throw new DuplicateUserException("email", "This email is already registered");
        }
        managed.setFullName(request.getFullName().trim());
        managed.setEmail(email);
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            managed.setPhone(request.getPhone().trim());
        } else {
            managed.setPhone(null);
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null
                    || !passwordEncoder.matches(request.getCurrentPassword(), managed.getPassword())) {
                throw new DuplicateUserException("currentPassword", "Current password is incorrect");
            }
            managed.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        User saved = userRepository.save(managed);
        saveSportLevels(saved, request.getSportLevels());
        return saved;
    }

    @Override
    @Transactional
    public User updatePhone(User user, String phone) {
        User managed = userRepository.findById(user.getId())
                .orElseThrow(() -> new DuplicateUserException("username", "Signed-in user was not found"));
        if (phone == null || phone.isBlank()) {
            throw new DuplicateUserException("phone", "Phone is required");
        }
        managed.setPhone(phone.trim());
        return userRepository.save(managed);
    }

    @Override
    @Transactional
    public User createStaff(AdminUserCreateRequest request) {
        Role role = request.getRole();
        if (role != Role.USER && role != Role.MANAGER && role != Role.COACH) {
            throw new DuplicateUserException("role", "Admins can create players, managers, or coaches only");
        }
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateUserException("username", "This username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException("email", "This email is already registered");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return username != null && userRepository.existsByUsernameIgnoreCase(username.trim());
    }

    @Override
    public boolean existsByEmail(String email) {
        return email != null && userRepository.existsByEmailIgnoreCase(email.trim());
    }

    @Override
    public Optional<User> findById(Long id) {
        return id == null ? Optional.empty() : userRepository.findById(id);
    }

    @Override
    @Transactional
    public void saveSportLevel(User user, ResourceType sport, String level) {
        if (user == null || sport == null) {
            return;
        }
        persistSportLevel(user, sport, level);
    }

    private void saveSportLevels(User user, Map<String, String> posted) {
        Map<String, String> values = posted == null ? Map.of() : posted;
        for (ResourceType sport : ResourceType.values()) {
            persistSportLevel(user, sport, values.get(sport.name()));
        }
    }

    private void persistSportLevel(User user, ResourceType sport, String raw) {
        if (raw == null || raw.isBlank()) {
            userSportLevelRepository.findByUser_IdAndSportType(user.getId(), sport)
                    .ifPresent(userSportLevelRepository::delete);
            return;
        }
        String code = raw.trim();
        if (!sportSkillLevelCatalog.isValid(sport, code)) {
            return;
        }
        UserSportLevel row = userSportLevelRepository.findByUser_IdAndSportType(user.getId(), sport)
                .orElseGet(UserSportLevel::new);
        row.setUser(user);
        row.setSportType(sport);
        row.setSkillLevel(code);
        userSportLevelRepository.save(row);
    }
}

package com.example.hackathoncodaro2026.repository;

import com.example.hackathoncodaro2026.model.UserSportLevel;
import com.example.hackathoncodaro2026.model.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSportLevelRepository extends JpaRepository<UserSportLevel, Long> {

    List<UserSportLevel> findByUser_Id(Long userId);

    Optional<UserSportLevel> findByUser_IdAndSportType(Long userId, ResourceType sportType);

    void deleteByUser_IdAndSportType(Long userId, ResourceType sportType);
}

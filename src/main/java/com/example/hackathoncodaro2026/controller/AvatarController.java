package com.example.hackathoncodaro2026.controller;

import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.service.AvatarStorageService;
import com.example.hackathoncodaro2026.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.TimeUnit;

@Controller
public class AvatarController {

    private final UserService userService;
    private final AvatarStorageService avatarStorageService;

    public AvatarController(UserService userService, AvatarStorageService avatarStorageService) {
        this.userService = userService;
        this.avatarStorageService = avatarStorageService;
    }

    @GetMapping("/avatars/{userId}")
    public ResponseEntity<Resource> avatar(@PathVariable Long userId) {
        User user = userService.findById(userId).orElse(null);
        AvatarStorageService.OptionalAvatar payload = avatarStorageService.load(user);
        CacheControl cache = payload.placeholder()
                ? CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic()
                : CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic();
        return ResponseEntity.ok()
                .contentType(payload.mediaType())
                .cacheControl(cache)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(payload.resource());
    }
}

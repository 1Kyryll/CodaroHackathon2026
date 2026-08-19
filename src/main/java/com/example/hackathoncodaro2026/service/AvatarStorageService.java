package com.example.hackathoncodaro2026.service;

import com.example.hackathoncodaro2026.model.User;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public interface AvatarStorageService {

    void store(User user, MultipartFile file);

    OptionalAvatar load(User user);

    record OptionalAvatar(Resource resource, MediaType mediaType, boolean placeholder) {
    }
}

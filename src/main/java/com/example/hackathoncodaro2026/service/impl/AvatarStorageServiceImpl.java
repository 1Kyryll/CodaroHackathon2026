package com.example.hackathoncodaro2026.service.impl;

import com.example.hackathoncodaro2026.exception.ReservationException;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.repository.UserRepository;
import com.example.hackathoncodaro2026.service.AuditLogService;
import com.example.hackathoncodaro2026.service.AvatarStorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AvatarStorageServiceImpl implements AvatarStorageService {

    private static final long MAX_BYTES = 1_048_576L;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp",
            MediaType.IMAGE_GIF_VALUE
    );
    private static final Map<String, String> EXTENSIONS = Map.of(
            MediaType.IMAGE_JPEG_VALUE, "jpg",
            MediaType.IMAGE_PNG_VALUE, "png",
            "image/webp", "webp",
            MediaType.IMAGE_GIF_VALUE, "gif"
    );

    private final Path root = Path.of("data", "avatars");
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public AvatarStorageServiceImpl(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public void store(User user, MultipartFile file) {
        if (user == null || user.getId() == null) {
            throw new ReservationException("That account could not be found");
        }
        if (file == null || file.isEmpty()) {
            return;
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            auditLogService.record(user, "AVATAR_UPLOAD", "USER", user.getId(), "REJECTED", Map.of("reason", "UNSUPPORTED_TYPE"));
            throw new ReservationException("Use a JPG, PNG, WEBP, or GIF photo");
        }
        if (file.getSize() > MAX_BYTES) {
            auditLogService.record(user, "AVATAR_UPLOAD", "USER", user.getId(), "REJECTED", Map.of("reason", "TOO_LARGE"));
            throw new ReservationException("Photo must be 1 MB or smaller");
        }
        try {
            Files.createDirectories(root);
            String extension = EXTENSIONS.get(contentType);
            String filename = user.getId() + "." + extension;
            Path target = root.resolve(filename).normalize();
            if (!target.startsWith(root.toAbsolutePath().normalize()) && !target.startsWith(root.normalize())) {
                auditLogService.record(user, "AVATAR_UPLOAD", "USER", user.getId(), "REJECTED", Map.of("reason", "PATH"));
                throw new ReservationException("That photo could not be saved");
            }
            if (user.getAvatarFilename() != null && !user.getAvatarFilename().isBlank()
                    && !user.getAvatarFilename().equals(filename)) {
                Files.deleteIfExists(root.resolve(user.getAvatarFilename()).normalize());
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            User managed = userRepository.findById(user.getId())
                    .orElseThrow(() -> new ReservationException("That account could not be found"));
            managed.setAvatarFilename(filename);
            userRepository.save(managed);
            user.setAvatarFilename(filename);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("changed", true);
            details.put("type", extension);
            auditLogService.record(user, "AVATAR_UPLOAD", "USER", user.getId(), "SUCCESS", details);
        } catch (IOException ex) {
            auditLogService.record(user, "AVATAR_UPLOAD", "USER", user.getId(), "REJECTED", Map.of("reason", "IO_ERROR"));
            throw new ReservationException("That photo could not be saved");
        }
    }

    @Override
    public OptionalAvatar load(User user) {
        if (user != null && user.getAvatarFilename() != null && !user.getAvatarFilename().isBlank()) {
            Path file = root.resolve(user.getAvatarFilename()).normalize();
            if (Files.isRegularFile(file)) {
                String name = user.getAvatarFilename().toLowerCase(Locale.ROOT);
                MediaType type = MediaType.IMAGE_JPEG;
                if (name.endsWith(".png")) {
                    type = MediaType.IMAGE_PNG;
                } else if (name.endsWith(".webp")) {
                    type = MediaType.parseMediaType("image/webp");
                } else if (name.endsWith(".gif")) {
                    type = MediaType.IMAGE_GIF;
                }
                return new OptionalAvatar(new FileSystemResource(file), type, false);
            }
        }
        String initials = user == null ? "C" : user.getInitials();
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
                  <rect width="64" height="64" rx="32" fill="#2E8B57"/>
                  <text x="32" y="40" text-anchor="middle" fill="#ffffff" font-size="22" font-family="Arial,sans-serif">%s</text>
                </svg>
                """.formatted(escape(initials));
        Resource resource = new ByteArrayResource(svg.getBytes(StandardCharsets.UTF_8));
        return new OptionalAvatar(resource, MediaType.parseMediaType("image/svg+xml"), true);
    }

    private String escape(String value) {
        if (value == null || value.isBlank()) {
            return "C";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

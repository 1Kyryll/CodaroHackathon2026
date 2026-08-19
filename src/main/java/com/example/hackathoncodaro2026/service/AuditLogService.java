package com.example.hackathoncodaro2026.service;

import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class AuditLogService {

    public static final Marker AUDIT_MARKER = MarkerFactory.getMarker("AUDIT");
    public static final String LOGGER_NAME = "AUDIT";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);
    private static final int MAX_VALUE_LENGTH = 180;

    public void record(
            String event,
            String actor,
            String role,
            String entityType,
            Object entityId,
            String result,
            Map<String, ?> details
    ) {
        String message = format(event, actor, role, entityType, entityId, result, details);
        if (isFailure(result)) {
            log.warn(AUDIT_MARKER, message);
        } else {
            log.info(AUDIT_MARKER, message);
        }
    }

    public void record(User actor, String event, String entityType, Object entityId, String result, Map<String, ?> details) {
        record(event, actorName(actor), roleName(actor), entityType, entityId, result, details);
    }

    public String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        return sanitize(authentication.getName());
    }

    public String currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return "";
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                return sanitize(value.substring(5));
            }
        }
        return "";
    }

    public String actorName(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return currentActor();
        }
        return sanitize(user.getUsername());
    }

    public String roleName(User user) {
        if (user == null || user.getRole() == null) {
            return currentRole();
        }
        return user.getRole().name();
    }

    public String roleName(Role role) {
        return role == null ? "" : role.name();
    }

    public String sanitize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder cleaned = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\r' || ch == '\n' || ch == '\t' || Character.isISOControl(ch)) {
                cleaned.append(' ');
            } else {
                cleaned.append(ch);
            }
        }
        String result = cleaned.toString().trim().replaceAll(" +", " ");
        if (result.length() > MAX_VALUE_LENGTH) {
            return result.substring(0, MAX_VALUE_LENGTH);
        }
        return result;
    }

    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        if (at <= 0 || at >= trimmed.length() - 1) {
            return "***";
        }
        String local = trimmed.substring(0, at);
        String domain = trimmed.substring(at + 1);
        String localMask = local.charAt(0) + "***";
        int dot = domain.lastIndexOf('.');
        String domainMask;
        if (dot > 0 && dot < domain.length() - 1) {
            domainMask = domain.charAt(0) + "***" + domain.substring(dot);
        } else {
            domainMask = domain.charAt(0) + "***";
        }
        return sanitize(localMask + "@" + domainMask).toLowerCase(Locale.ROOT);
    }

    public String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < phone.length(); i++) {
            char ch = phone.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digits.append(ch);
            }
        }
        if (digits.length() < 2) {
            return "***";
        }
        return "***" + digits.substring(digits.length() - 2);
    }

    private boolean isFailure(String result) {
        if (result == null) {
            return false;
        }
        String normalized = result.trim().toUpperCase(Locale.ROOT);
        return "FAILURE".equals(normalized) || "REJECTED".equals(normalized) || "ERROR".equals(normalized);
    }

    private String format(
            String event,
            String actor,
            String role,
            String entityType,
            Object entityId,
            String result,
            Map<String, ?> details
    ) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("event", sanitize(event));
        fields.put("actor", sanitize(actor));
        fields.put("role", sanitize(role));
        fields.put("entityType", sanitize(entityType));
        if (entityId != null) {
            fields.put("entityId", sanitize(String.valueOf(entityId)));
        }
        fields.put("result", sanitize(result));
        if (details != null) {
            for (Map.Entry<String, ?> entry : details.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                    continue;
                }
                String key = sanitize(entry.getKey()).replace(" ", "");
                if (key.isEmpty()) {
                    continue;
                }
                fields.put(key, sanitize(String.valueOf(entry.getValue())));
            }
        }
        StringBuilder message = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            if (!first) {
                message.append(' ');
            }
            first = false;
            message.append(entry.getKey()).append('=').append(quote(entry.getValue()));
        }
        return message.toString();
    }

    private String quote(String value) {
        if (value.indexOf(' ') >= 0 || value.indexOf('=') >= 0 || value.indexOf('"') >= 0) {
            return '"' + value.replace("\"", "'") + '"';
        }
        return value;
    }
}

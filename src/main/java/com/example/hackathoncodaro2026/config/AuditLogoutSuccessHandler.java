package com.example.hackathoncodaro2026.config;

import com.example.hackathoncodaro2026.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class AuditLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final AuditLogService auditLogService;

    public AuditLogoutSuccessHandler(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
        setDefaultTargetUrl("/login?logout");
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        String actor = authentication == null ? "anonymous" : auditLogService.sanitize(authentication.getName());
        String role = "";
        if (authentication != null && authentication.getAuthorities() != null) {
            role = authentication.getAuthorities().stream()
                    .map(granted -> granted.getAuthority())
                    .filter(value -> value != null && value.startsWith("ROLE_"))
                    .map(value -> value.substring(5))
                    .findFirst()
                    .orElse("");
        }
        auditLogService.record(
                "LOGOUT",
                actor,
                role,
                "SESSION",
                null,
                "SUCCESS",
                Map.of()
        );
        super.onLogoutSuccess(request, response, authentication);
    }
}

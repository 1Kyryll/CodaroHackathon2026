package com.example.hackathoncodaro2026.config;

import com.example.hackathoncodaro2026.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class AuditAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AuditLogService auditLogService;

    public AuditAuthenticationFailureHandler(AuditLogService auditLogService) {
        super("/login?error");
        this.auditLogService = auditLogService;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String actor = auditLogService.sanitize(request.getParameter("username"));
        if (actor.isEmpty()) {
            actor = "unknown";
        }
        String reason = "BAD_CREDENTIALS";
        if (exception instanceof DisabledException) {
            reason = "DISABLED";
        } else if (exception instanceof LockedException) {
            reason = "LOCKED";
        }
        auditLogService.record(
                "LOGIN",
                actor,
                "",
                "SESSION",
                null,
                "FAILURE",
                Map.of("reason", reason)
        );
        super.onAuthenticationFailure(request, response, exception);
    }
}

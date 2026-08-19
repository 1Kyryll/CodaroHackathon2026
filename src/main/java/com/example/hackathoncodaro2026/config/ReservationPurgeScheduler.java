package com.example.hackathoncodaro2026.config;

import com.example.hackathoncodaro2026.service.AuditLogService;
import com.example.hackathoncodaro2026.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ReservationPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationPurgeScheduler.class);

    private final ReservationService reservationService;
    private final AuditLogService auditLogService;

    public ReservationPurgeScheduler(ReservationService reservationService, AuditLogService auditLogService) {
        this.reservationService = reservationService;
        this.auditLogService = auditLogService;
    }

    @Scheduled(cron = "0 59 23 * * *", zone = "Europe/Warsaw")
    public void purgeEndedOlderThanOneMonth() {
        try {
            int removed = reservationService.deleteEndedOlderThanOneMonth();
            log.info("Removed {} reservations that ended more than one month ago", removed);
        } catch (Exception ex) {
            log.error("Reservation purge failed", ex);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("reason", ex.getClass().getSimpleName());
            auditLogService.record(
                    "RESERVATION_PURGE",
                    "system",
                    "SCHEDULER",
                    "RESERVATION",
                    null,
                    "FAILURE",
                    details
            );
        }
    }
}

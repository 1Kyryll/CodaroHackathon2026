package com.example.hackathoncodaro2026.service;

import com.example.hackathoncodaro2026.model.Notification;
import com.example.hackathoncodaro2026.model.User;
import com.example.hackathoncodaro2026.model.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    Notification create(User recipient, NotificationType type, String title, String message, Long reservationId);

    List<Notification> findFor(User recipient);

    long unreadCount(User recipient);

    void markRead(User actor, Long notificationId);

    void markAllRead(User actor);
}

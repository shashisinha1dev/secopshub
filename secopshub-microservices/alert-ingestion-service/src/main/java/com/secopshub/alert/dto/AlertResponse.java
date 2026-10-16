package com.secopshub.alert.dto;

import com.secopshub.alert.domain.SecurityAlert;
import java.time.LocalDateTime;

public record AlertResponse(
        String alertId,
        String externalAlertId,
        String source,
        String title,
        String incidentType,
        String natureOfViolation,
        String severity,
        String status,
        String createdIncidentId,
        String lastError,
        LocalDateTime reportedAt,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AlertResponse from(SecurityAlert alert) {
        return new AlertResponse(alert.getAlertId(), alert.getExternalAlertId(), alert.getSource(),
                alert.getTitle(), alert.getIncidentType(), alert.getNatureOfViolation(), alert.getSeverity(),
                alert.getStatus().name(), alert.getCreatedIncidentId(), alert.getLastError(), alert.getReportedAt(),
                alert.getProcessedAt(), alert.getCreatedAt(), alert.getUpdatedAt());
    }
}

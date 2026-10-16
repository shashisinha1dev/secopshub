package com.secopshub.contracts;

import java.time.LocalDateTime;

/** Versionable event contract published when the alert-ingestion service accepts an alert. */
public record AlertReceivedEvent(
        String alertId,
        String externalAlertId,
        String source,
        String title,
        String description,
        String incidentType,
        String natureOfViolation,
        String severity,
        String classificationStatus,
        String reportedBy,
        String assignedTo,
        String businessUnit,
        String policyReference,
        LocalDateTime occurredAt,
        LocalDateTime detectedAt,
        LocalDateTime reportedAt,
        String businessImpact,
        String rootCause,
        String correctiveAction
) { }

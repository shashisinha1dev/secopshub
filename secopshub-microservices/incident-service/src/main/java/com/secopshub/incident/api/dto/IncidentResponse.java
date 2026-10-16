package com.secopshub.incident.api.dto;

import com.secopshub.incident.domain.*;
import java.time.LocalDateTime;

public record IncidentResponse(
        String incidentId, String sourceAlertId, String title, String description, String incidentType,
        String natureOfViolation, Severity severity, IncidentStatus status,
        ClassificationStatus classificationStatus, String reportedBy, String assignedTo,
        String businessUnit, String policyReference, LocalDateTime occurredAt,
        LocalDateTime detectedAt, LocalDateTime reportedAt, String fiscalYear,
        String reportingQuarter, String businessImpact, String rootCause,
        String correctiveAction, LocalDateTime resolvedAt, LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static IncidentResponse from(Incident i) {
        return new IncidentResponse(i.getIncidentId(), i.getSourceAlertId(), i.getTitle(), i.getDescription(), i.getIncidentType(),
                i.getNatureOfViolation(), i.getSeverity(), i.getStatus(), i.getClassificationStatus(),
                i.getReportedBy(), i.getAssignedTo(), i.getBusinessUnit(), i.getPolicyReference(),
                i.getOccurredAt(), i.getDetectedAt(), i.getReportedAt(), i.getFiscalYear(),
                i.getReportingQuarter(), i.getBusinessImpact(), i.getRootCause(), i.getCorrectiveAction(),
                i.getResolvedAt(), i.getCreatedAt(), i.getUpdatedAt());
    }
}

package com.secopshub.incident.api.dto;

import com.secopshub.incident.domain.ClassificationStatus;
import com.secopshub.incident.domain.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateIncidentRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 3000) String description,
        @NotBlank @Size(max = 80) String incidentType,
        @NotBlank @Size(max = 80) String natureOfViolation,
        @NotNull Severity severity,
        ClassificationStatus classificationStatus,
        @NotBlank @Size(max = 180) String reportedBy,
        @Size(max = 180) String assignedTo,
        @Size(max = 120) String businessUnit,
        @Size(max = 120) String policyReference,
        LocalDateTime occurredAt,
        LocalDateTime detectedAt,
        LocalDateTime reportedAt,
        @Size(max = 2000) String businessImpact,
        @Size(max = 2000) String rootCause,
        @Size(max = 2000) String correctiveAction
) { }

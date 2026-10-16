package com.secopshub.alert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record SubmitAlertRequest(
        @NotBlank @Size(max = 120) String externalAlertId,
        @NotBlank @Size(max = 100) String source,
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 3000) String description,
        @NotBlank @Size(max = 80) String incidentType,
        @NotBlank @Size(max = 80) String natureOfViolation,
        @NotBlank @Pattern(regexp = "(?i)LOW|MEDIUM|HIGH|CRITICAL", message = "must be LOW, MEDIUM, HIGH, or CRITICAL") String severity,
        @Pattern(regexp = "(?i)SUSPECTED|CONFIRMED|NOT_SUBSTANTIATED", message = "must be SUSPECTED, CONFIRMED, or NOT_SUBSTANTIATED") String classificationStatus,
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

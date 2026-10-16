package com.secopshub.incident.api.dto;

import com.secopshub.incident.domain.IncidentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull IncidentStatus status, @NotBlank String performedBy, @NotBlank String rationale) { }

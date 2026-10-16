package com.secopshub.incident.api.dto;

import com.secopshub.incident.domain.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateSeverityRequest(@NotNull Severity severity, @NotBlank String performedBy, @NotBlank String rationale) { }

package com.secopshub.incident.api.dto;

import com.secopshub.incident.domain.RelationshipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRelationshipRequest(@NotBlank String relatedIncidentId, @NotNull RelationshipType relationshipType, @NotBlank String createdBy) { }

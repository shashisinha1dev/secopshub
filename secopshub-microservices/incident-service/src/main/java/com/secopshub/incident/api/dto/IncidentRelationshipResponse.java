package com.secopshub.incident.api.dto;

import com.secopshub.incident.domain.IncidentRelationship;
import com.secopshub.incident.domain.RelationshipType;
import java.time.LocalDateTime;

public record IncidentRelationshipResponse(Long id, String sourceIncidentId, String relatedIncidentId,
                                           RelationshipType relationshipType, String createdBy, LocalDateTime createdAt) {
    public static IncidentRelationshipResponse from(IncidentRelationship r) {
        return new IncidentRelationshipResponse(r.getId(), r.getSourceIncident().getIncidentId(),
                r.getRelatedIncident().getIncidentId(), r.getRelationshipType(), r.getCreatedBy(), r.getCreatedAt());
    }
}

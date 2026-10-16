package com.secopshub.incident.api.dto;

import com.secopshub.incident.domain.IncidentAuditEvent;
import java.time.LocalDateTime;

public record AuditEventResponse(Long id, String incidentId, String action, String oldValue,
                                 String newValue, String performedBy, String rationale, LocalDateTime occurredAt) {
    public static AuditEventResponse from(IncidentAuditEvent e) {
        return new AuditEventResponse(e.getId(), e.getIncident().getIncidentId(), e.getAction(), e.getOldValue(),
                e.getNewValue(), e.getPerformedBy(), e.getRationale(), e.getOccurredAt());
    }
}

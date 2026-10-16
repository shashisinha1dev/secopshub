package com.secopshub.incident.repository;

import com.secopshub.incident.domain.IncidentAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentAuditEventRepository extends JpaRepository<IncidentAuditEvent, Long> {
    List<IncidentAuditEvent> findByIncident_IncidentIdOrderByOccurredAtDesc(String incidentId);
}

package com.secopshub.incident.repository;

import com.secopshub.incident.domain.IncidentRelationship;
import com.secopshub.incident.domain.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentRelationshipRepository extends JpaRepository<IncidentRelationship, Long> {
    List<IncidentRelationship> findBySourceIncident_IncidentIdOrRelatedIncident_IncidentIdOrderByCreatedAtDesc(String sourceId, String relatedId);
    boolean existsBySourceIncident_IncidentIdAndRelatedIncident_IncidentIdAndRelationshipType(String sourceId, String relatedId, RelationshipType type);
}

package com.secopshub.incident.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_relationships", uniqueConstraints = @UniqueConstraint(
        name = "uk_incident_relationship", columnNames = {"source_incident_id", "related_incident_id", "relationship_type"}))
public class IncidentRelationship {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "source_incident_id", nullable = false)
    private Incident sourceIncident;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "related_incident_id", nullable = false)
    private Incident relatedIncident;
    @Enumerated(EnumType.STRING) @Column(name = "relationship_type", nullable = false, length = 32)
    private RelationshipType relationshipType;
    @Column(nullable = false, length = 180) private String createdBy;
    @Column(nullable = false) private LocalDateTime createdAt;

    protected IncidentRelationship() { }
    public IncidentRelationship(Incident sourceIncident, Incident relatedIncident, RelationshipType relationshipType, String createdBy) {
        this.sourceIncident = sourceIncident; this.relatedIncident = relatedIncident;
        this.relationshipType = relationshipType; this.createdBy = createdBy; this.createdAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public Incident getSourceIncident() { return sourceIncident; }
    public Incident getRelatedIncident() { return relatedIncident; }
    public RelationshipType getRelationshipType() { return relationshipType; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

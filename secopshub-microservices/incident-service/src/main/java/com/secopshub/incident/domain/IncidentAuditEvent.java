package com.secopshub.incident.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_audit_events", indexes = @Index(name = "idx_audit_incident_time", columnList = "incident_id, occurred_at"))
public class IncidentAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;
    @Column(nullable = false, length = 48) private String action;
    @Column(length = 500) private String oldValue;
    @Column(length = 500) private String newValue;
    @Column(nullable = false, length = 180) private String performedBy;
    @Column(length = 2000) private String rationale;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;

    protected IncidentAuditEvent() { }
    public IncidentAuditEvent(Incident incident, String action, String oldValue, String newValue, String performedBy, String rationale) {
        this.incident = incident; this.action = action; this.oldValue = oldValue; this.newValue = newValue;
        this.performedBy = performedBy; this.rationale = rationale; this.occurredAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public String getAction() { return action; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getPerformedBy() { return performedBy; }
    public String getRationale() { return rationale; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}

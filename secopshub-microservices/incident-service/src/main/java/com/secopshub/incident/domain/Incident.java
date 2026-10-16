package com.secopshub.incident.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "incidents", indexes = {
        @Index(name = "idx_incidents_reported_period", columnList = "fiscal_year, reporting_quarter"),
        @Index(name = "idx_incidents_status_severity", columnList = "status, severity")
})
public class Incident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false, unique = true, length = 24)
    private String incidentId;
    @Column(name = "source_alert_id", unique = true, length = 32)
    private String sourceAlertId;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, length = 3000) private String description;
    @Column(nullable = false, length = 80) private String incidentType;
    @Column(nullable = false, length = 80) private String natureOfViolation;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Severity severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private IncidentStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ClassificationStatus classificationStatus;
    @Column(nullable = false, length = 180) private String reportedBy;
    @Column(length = 180) private String assignedTo;
    @Column(length = 120) private String businessUnit;
    @Column(length = 120) private String policyReference;
    private LocalDateTime occurredAt;
    private LocalDateTime detectedAt;
    @Column(nullable = false) private LocalDateTime reportedAt;
    @Column(nullable = false, length = 10) private String fiscalYear;
    @Column(nullable = false, length = 2) private String reportingQuarter;
    @Column(length = 2000) private String businessImpact;
    @Column(length = 2000) private String rootCause;
    @Column(length = 2000) private String correctiveAction;
    private LocalDateTime resolvedAt;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    public Incident() { }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (reportedAt == null) reportedAt = now;
        if (incidentId == null || incidentId.isBlank()) {
            incidentId = "SEC-" + reportedAt.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        ReportingPeriod period = ReportingPeriod.from(reportedAt);
        fiscalYear = period.fiscalYear();
        reportingQuarter = period.quarter();
        if (status == null) status = IncidentStatus.NEW;
        if (classificationStatus == null) classificationStatus = ClassificationStatus.SUSPECTED;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getIncidentId() { return incidentId; }
    public String getSourceAlertId() { return sourceAlertId; }
    public void setSourceAlertId(String sourceAlertId) { this.sourceAlertId = sourceAlertId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }
    public String getNatureOfViolation() { return natureOfViolation; }
    public void setNatureOfViolation(String natureOfViolation) { this.natureOfViolation = natureOfViolation; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public ClassificationStatus getClassificationStatus() { return classificationStatus; }
    public void setClassificationStatus(ClassificationStatus classificationStatus) { this.classificationStatus = classificationStatus; }
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }
    public String getPolicyReference() { return policyReference; }
    public void setPolicyReference(String policyReference) { this.policyReference = policyReference; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public String getFiscalYear() { return fiscalYear; }
    public String getReportingQuarter() { return reportingQuarter; }
    public String getBusinessImpact() { return businessImpact; }
    public void setBusinessImpact(String businessImpact) { this.businessImpact = businessImpact; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getCorrectiveAction() { return correctiveAction; }
    public void setCorrectiveAction(String correctiveAction) { this.correctiveAction = correctiveAction; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

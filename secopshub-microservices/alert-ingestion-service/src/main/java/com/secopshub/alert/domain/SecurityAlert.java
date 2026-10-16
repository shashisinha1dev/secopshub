package com.secopshub.alert.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_alerts", indexes = {
        @Index(name = "idx_alerts_status_created", columnList = "status, created_at")
})
public class SecurityAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false, unique = true, length = 32)
    private String alertId;
    @Column(name = "external_alert_id", nullable = false, unique = true, length = 120)
    private String externalAlertId;
    @Column(nullable = false, length = 100) private String source;
    @Column(nullable = false, length = 180) private String title;
    @Column(nullable = false, length = 3000) private String description;
    @Column(nullable = false, length = 80) private String incidentType;
    @Column(nullable = false, length = 80) private String natureOfViolation;
    @Column(nullable = false, length = 16) private String severity;
    @Column(nullable = false, length = 24) private String classificationStatus;
    @Column(nullable = false, length = 180) private String reportedBy;
    @Column(length = 180) private String assignedTo;
    @Column(length = 120) private String businessUnit;
    @Column(length = 120) private String policyReference;
    private LocalDateTime occurredAt;
    private LocalDateTime detectedAt;
    @Column(nullable = false) private LocalDateTime reportedAt;
    @Column(length = 2000) private String businessImpact;
    @Column(length = 2000) private String rootCause;
    @Column(length = 2000) private String correctiveAction;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private AlertProcessingStatus status;
    @Column(length = 24) private String createdIncidentId;
    @Column(length = 1000) private String lastError;
    private LocalDateTime processedAt;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    public SecurityAlert() { }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (alertId == null || alertId.isBlank()) {
            alertId = "ALT-" + now.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (reportedAt == null) reportedAt = now;
        if (status == null) status = AlertProcessingStatus.RECEIVED;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getAlertId() { return alertId; }
    public String getExternalAlertId() { return externalAlertId; }
    public void setExternalAlertId(String value) { externalAlertId = value; }
    public String getSource() { return source; }
    public void setSource(String value) { source = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String value) { incidentType = value; }
    public String getNatureOfViolation() { return natureOfViolation; }
    public void setNatureOfViolation(String value) { natureOfViolation = value; }
    public String getSeverity() { return severity; }
    public void setSeverity(String value) { severity = value; }
    public String getClassificationStatus() { return classificationStatus; }
    public void setClassificationStatus(String value) { classificationStatus = value; }
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String value) { reportedBy = value; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String value) { assignedTo = value; }
    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String value) { businessUnit = value; }
    public String getPolicyReference() { return policyReference; }
    public void setPolicyReference(String value) { policyReference = value; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime value) { occurredAt = value; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime value) { detectedAt = value; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime value) { reportedAt = value; }
    public String getBusinessImpact() { return businessImpact; }
    public void setBusinessImpact(String value) { businessImpact = value; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String value) { rootCause = value; }
    public String getCorrectiveAction() { return correctiveAction; }
    public void setCorrectiveAction(String value) { correctiveAction = value; }
    public AlertProcessingStatus getStatus() { return status; }
    public void setStatus(AlertProcessingStatus value) { status = value; }
    public String getCreatedIncidentId() { return createdIncidentId; }
    public void setCreatedIncidentId(String value) { createdIncidentId = value; }
    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime value) { processedAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

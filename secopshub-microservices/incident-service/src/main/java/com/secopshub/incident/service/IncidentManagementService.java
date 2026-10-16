package com.secopshub.incident.service;

import com.secopshub.incident.api.dto.*;
import com.secopshub.incident.domain.*;
import com.secopshub.incident.error.ResourceNotFoundException;
import com.secopshub.incident.repository.*;
import com.secopshub.contracts.AlertReceivedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IncidentManagementService {
    private final IncidentRepository incidentRepository;
    private final IncidentRelationshipRepository relationshipRepository;
    private final IncidentAuditEventRepository auditRepository;

    public IncidentManagementService(IncidentRepository incidentRepository,
                                     IncidentRelationshipRepository relationshipRepository,
                                     IncidentAuditEventRepository auditRepository) {
        this.incidentRepository = incidentRepository;
        this.relationshipRepository = relationshipRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        Incident i = new Incident();
        i.setTitle(request.title().trim());
        i.setDescription(request.description().trim());
        i.setIncidentType(request.incidentType().trim().toUpperCase(Locale.ROOT));
        i.setNatureOfViolation(request.natureOfViolation().trim().toUpperCase(Locale.ROOT));
        i.setSeverity(request.severity());
        i.setStatus(IncidentStatus.NEW);
        i.setClassificationStatus(request.classificationStatus() == null ? ClassificationStatus.SUSPECTED : request.classificationStatus());
        i.setReportedBy(request.reportedBy().trim());
        i.setAssignedTo(clean(request.assignedTo()));
        i.setBusinessUnit(clean(request.businessUnit()));
        i.setPolicyReference(clean(request.policyReference()));
        i.setOccurredAt(request.occurredAt());
        i.setDetectedAt(request.detectedAt());
        i.setReportedAt(request.reportedAt() == null ? LocalDateTime.now() : request.reportedAt());
        i.setBusinessImpact(clean(request.businessImpact()));
        i.setRootCause(clean(request.rootCause()));
        i.setCorrectiveAction(clean(request.correctiveAction()));
        Incident saved = incidentRepository.save(i);
        auditRepository.save(new IncidentAuditEvent(saved, "INCIDENT_CREATED", null, saved.getStatus().name(), saved.getReportedBy(), "Incident created"));
        return IncidentResponse.from(saved);
    }

    /** Idempotently creates an incident from a queued alert. */
    @Transactional
    public String createFromAlert(AlertReceivedEvent event) {
        if (event.alertId() == null || event.alertId().isBlank()) {
            throw new IllegalArgumentException("Alert event must include alertId.");
        }
        var existing = incidentRepository.findBySourceAlertId(event.alertId());
        if (existing.isPresent()) return existing.get().getIncidentId();

        Incident incident = new Incident();
        incident.setSourceAlertId(event.alertId());
        incident.setTitle(event.title().trim());
        incident.setDescription(event.description().trim());
        incident.setIncidentType(event.incidentType().trim().toUpperCase(Locale.ROOT));
        incident.setNatureOfViolation(event.natureOfViolation().trim().toUpperCase(Locale.ROOT));
        incident.setSeverity(Severity.valueOf(event.severity().trim().toUpperCase(Locale.ROOT)));
        incident.setStatus(IncidentStatus.NEW);
        incident.setClassificationStatus(event.classificationStatus() == null || event.classificationStatus().isBlank()
                ? ClassificationStatus.SUSPECTED
                : ClassificationStatus.valueOf(event.classificationStatus().trim().toUpperCase(Locale.ROOT)));
        incident.setReportedBy(event.reportedBy() == null || event.reportedBy().isBlank()
                ? "alert-ingestion-service" : event.reportedBy().trim());
        incident.setAssignedTo(clean(event.assignedTo()));
        incident.setBusinessUnit(clean(event.businessUnit()));
        incident.setPolicyReference(clean(event.policyReference()));
        incident.setOccurredAt(event.occurredAt());
        incident.setDetectedAt(event.detectedAt());
        incident.setReportedAt(event.reportedAt() == null ? LocalDateTime.now() : event.reportedAt());
        incident.setBusinessImpact(clean(event.businessImpact()));
        incident.setRootCause(clean(event.rootCause()));
        incident.setCorrectiveAction(clean(event.correctiveAction()));
        Incident saved = incidentRepository.save(incident);
        auditRepository.save(new IncidentAuditEvent(saved, "INCIDENT_CREATED_FROM_ALERT", null,
                saved.getStatus().name(), saved.getReportedBy(), "Created from alert " + event.externalAlertId()));
        return saved.getIncidentId();
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(String incidentId) { return IncidentResponse.from(requireIncident(incidentId)); }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> search(Severity severity, IncidentStatus status, String natureOfViolation,
                                         String incidentType, String fiscalYear, String quarter, String assignedTo,
                                         Pageable pageable) {
        return incidentRepository.search(severity, status, clean(natureOfViolation), clean(incidentType),
                clean(fiscalYear), clean(quarter), clean(assignedTo), pageable).map(IncidentResponse::from);
    }

    @Transactional
    public IncidentResponse updateSeverity(String incidentId, UpdateSeverityRequest request) {
        Incident i = requireIncident(incidentId);
        Severity old = i.getSeverity();
        if (old == request.severity()) return IncidentResponse.from(i);
        i.setSeverity(request.severity());
        Incident saved = incidentRepository.save(i);
        auditRepository.save(new IncidentAuditEvent(saved, "SEVERITY_CHANGED", old.name(), request.severity().name(), request.performedBy(), request.rationale()));
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentResponse updateStatus(String incidentId, UpdateStatusRequest request) {
        Incident i = requireIncident(incidentId);
        IncidentStatus old = i.getStatus();
        IncidentStatus next = request.status();
        if (old == next) return IncidentResponse.from(i);
        if (!isAllowedTransition(old, next)) {
            throw new IllegalArgumentException("Status transition from " + old + " to " + next + " is not allowed.");
        }
        i.setStatus(next);
        if (next == IncidentStatus.RESOLVED) i.setResolvedAt(LocalDateTime.now());
        if (old == IncidentStatus.RESOLVED && next == IncidentStatus.INVESTIGATING) i.setResolvedAt(null);
        Incident saved = incidentRepository.save(i);
        auditRepository.save(new IncidentAuditEvent(saved, "STATUS_CHANGED", old.name(), next.name(), request.performedBy(), request.rationale()));
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentRelationshipResponse addRelationship(String incidentId, CreateRelationshipRequest request) {
        if (incidentId.equalsIgnoreCase(request.relatedIncidentId())) {
            throw new IllegalArgumentException("An incident cannot be related to itself.");
        }
        Incident source = requireIncident(incidentId);
        Incident related = requireIncident(request.relatedIncidentId());
        boolean exists = relationshipRepository.existsBySourceIncident_IncidentIdAndRelatedIncident_IncidentIdAndRelationshipType(
                source.getIncidentId(), related.getIncidentId(), request.relationshipType())
                || relationshipRepository.existsBySourceIncident_IncidentIdAndRelatedIncident_IncidentIdAndRelationshipType(
                related.getIncidentId(), source.getIncidentId(), request.relationshipType());
        if (exists) throw new IllegalArgumentException("This relationship already exists.");
        IncidentRelationship saved = relationshipRepository.save(new IncidentRelationship(source, related, request.relationshipType(), request.createdBy().trim()));
        return IncidentRelationshipResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentRelationshipResponse> relationships(String incidentId) {
        requireIncident(incidentId);
        return relationshipRepository.findBySourceIncident_IncidentIdOrRelatedIncident_IncidentIdOrderByCreatedAtDesc(incidentId, incidentId)
                .stream().map(IncidentRelationshipResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> auditEvents(String incidentId) {
        requireIncident(incidentId);
        return auditRepository.findByIncident_IncidentIdOrderByOccurredAtDesc(incidentId).stream().map(AuditEventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public QuarterlyReportResponse quarterlyReport(String fiscalYear, String quarter) {
        if (fiscalYear == null || fiscalYear.isBlank() || quarter == null || quarter.isBlank()) {
            ReportingPeriod current = ReportingPeriod.from(LocalDateTime.now());
            if (fiscalYear == null || fiscalYear.isBlank()) fiscalYear = current.fiscalYear();
            if (quarter == null || quarter.isBlank()) quarter = current.quarter();
        }
        fiscalYear = fiscalYear.trim().toUpperCase(Locale.ROOT);
        quarter = quarter.trim().toUpperCase(Locale.ROOT);
        if (!fiscalYear.matches("FY\\d{4}-\\d{2}")) throw new IllegalArgumentException("fiscalYear must look like FY2026-27.");
        if (!Set.of("Q1", "Q2", "Q3", "Q4").contains(quarter)) throw new IllegalArgumentException("quarter must be Q1, Q2, Q3, or Q4.");
        List<Incident> incidents = incidentRepository.findByFiscalYearAndReportingQuarterOrderByReportedAtDesc(fiscalYear, quarter);
        return new QuarterlyReportResponse(fiscalYear, quarter, incidents.size(),
                countBy(incidents, i -> i.getSeverity().name()),
                countBy(incidents, i -> i.getStatus().name()),
                countBy(incidents, Incident::getNatureOfViolation));
    }

    private <K> Map<String, Long> countBy(List<Incident> incidents, Function<Incident, K> classifier) {
        Map<String, Long> result = incidents.stream().collect(Collectors.groupingBy(i -> String.valueOf(classifier.apply(i)), TreeMap::new, Collectors.counting()));
        return result;
    }

    private Incident requireIncident(String incidentId) {
        return incidentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));
    }

    private boolean isAllowedTransition(IncidentStatus from, IncidentStatus to) {
        return switch (from) {
            case NEW -> to == IncidentStatus.TRIAGED;
            case TRIAGED -> to == IncidentStatus.INVESTIGATING;
            case INVESTIGATING -> to == IncidentStatus.CONTAINMENT || to == IncidentStatus.RESOLVED;
            case CONTAINMENT -> to == IncidentStatus.RECOVERY;
            case RECOVERY -> to == IncidentStatus.RESOLVED;
            case RESOLVED -> to == IncidentStatus.CLOSED || to == IncidentStatus.INVESTIGATING;
            case CLOSED -> false;
        };
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}

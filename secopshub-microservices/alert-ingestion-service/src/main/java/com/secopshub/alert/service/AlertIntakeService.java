package com.secopshub.alert.service;

import com.secopshub.alert.domain.AlertProcessingStatus;
import com.secopshub.alert.domain.SecurityAlert;
import com.secopshub.alert.dto.AlertResponse;
import com.secopshub.alert.dto.AlertSubmissionResult;
import com.secopshub.alert.dto.SubmitAlertRequest;
import com.secopshub.alert.repository.SecurityAlertRepository;
import com.secopshub.contracts.AlertReceivedEvent;
import com.secopshub.contracts.MessagingNames;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AlertIntakeService {
    private final SecurityAlertRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public AlertIntakeService(SecurityAlertRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public AlertSubmissionResult submit(SubmitAlertRequest request) {
        var existing = repository.findByExternalAlertId(request.externalAlertId().trim());
        if (existing.isPresent()) {
            SecurityAlert alert = existing.get();
            if (alert.getStatus() == AlertProcessingStatus.PUBLISH_FAILED || alert.getStatus() == AlertProcessingStatus.RECEIVED) {
                return publish(alert, true);
            }
            return new AlertSubmissionResult(AlertResponse.from(alert), false, true);
        }

        SecurityAlert alert = fromRequest(request);
        alert.setStatus(AlertProcessingStatus.RECEIVED);
        alert = repository.save(alert);
        return publish(alert, true);
    }

    public AlertResponse getByExternalAlertId(String externalAlertId) {
        return repository.findByExternalAlertId(externalAlertId)
                .map(AlertResponse::from)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found: " + externalAlertId));
    }

    public List<AlertResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(AlertResponse::from).toList();
    }

    public void markIncidentCreated(String alertId, String incidentId, LocalDateTime processedAt) {
        repository.findByAlertId(alertId).ifPresent(alert -> {
            alert.setStatus(AlertProcessingStatus.INCIDENT_CREATED);
            alert.setCreatedIncidentId(incidentId);
            alert.setProcessedAt(processedAt == null ? LocalDateTime.now() : processedAt);
            alert.setLastError(null);
            repository.save(alert);
        });
    }

    private AlertSubmissionResult publish(SecurityAlert alert, boolean isNewOrRetried) {
        alert.setStatus(AlertProcessingStatus.QUEUED);
        alert.setLastError(null);
        alert = repository.save(alert);
        try {
            rabbitTemplate.convertAndSend(MessagingNames.ALERT_EXCHANGE, MessagingNames.ALERT_ROUTING_KEY, toEvent(alert));
            return new AlertSubmissionResult(AlertResponse.from(alert), isNewOrRetried, true);
        } catch (AmqpException ex) {
            alert.setStatus(AlertProcessingStatus.PUBLISH_FAILED);
            alert.setLastError(safeError(ex.getMessage()));
            SecurityAlert failed = repository.save(alert);
            return new AlertSubmissionResult(AlertResponse.from(failed), isNewOrRetried, false);
        }
    }

    private SecurityAlert fromRequest(SubmitAlertRequest request) {
        SecurityAlert alert = new SecurityAlert();
        alert.setExternalAlertId(request.externalAlertId().trim());
        alert.setSource(request.source().trim());
        alert.setTitle(request.title().trim());
        alert.setDescription(request.description().trim());
        alert.setIncidentType(request.incidentType().trim().toUpperCase(Locale.ROOT));
        alert.setNatureOfViolation(request.natureOfViolation().trim().toUpperCase(Locale.ROOT));
        alert.setSeverity(request.severity().trim().toUpperCase(Locale.ROOT));
        alert.setClassificationStatus(request.classificationStatus() == null || request.classificationStatus().isBlank()
                ? "SUSPECTED" : request.classificationStatus().trim().toUpperCase(Locale.ROOT));
        alert.setReportedBy(request.reportedBy().trim());
        alert.setAssignedTo(clean(request.assignedTo()));
        alert.setBusinessUnit(clean(request.businessUnit()));
        alert.setPolicyReference(clean(request.policyReference()));
        alert.setOccurredAt(request.occurredAt());
        alert.setDetectedAt(request.detectedAt());
        alert.setReportedAt(request.reportedAt() == null ? LocalDateTime.now() : request.reportedAt());
        alert.setBusinessImpact(clean(request.businessImpact()));
        alert.setRootCause(clean(request.rootCause()));
        alert.setCorrectiveAction(clean(request.correctiveAction()));
        return alert;
    }

    private AlertReceivedEvent toEvent(SecurityAlert alert) {
        return new AlertReceivedEvent(alert.getAlertId(), alert.getExternalAlertId(), alert.getSource(),
                alert.getTitle(), alert.getDescription(), alert.getIncidentType(), alert.getNatureOfViolation(),
                alert.getSeverity(), alert.getClassificationStatus(), alert.getReportedBy(), alert.getAssignedTo(),
                alert.getBusinessUnit(), alert.getPolicyReference(), alert.getOccurredAt(), alert.getDetectedAt(),
                alert.getReportedAt(), alert.getBusinessImpact(), alert.getRootCause(), alert.getCorrectiveAction());
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String safeError(String message) {
        if (message == null || message.isBlank()) return "Message broker unavailable; retry this alert using the same externalAlertId.";
        return message.length() > 950 ? message.substring(0, 950) : message;
    }
}

package com.secopshub.incident.service;

import com.secopshub.contracts.AlertReceivedEvent;
import com.secopshub.incident.api.dto.CreateIncidentRequest;
import com.secopshub.incident.api.dto.IncidentResponse;
import com.secopshub.incident.api.dto.QuarterlyReportResponse;
import com.secopshub.incident.api.dto.UpdateStatusRequest;
import com.secopshub.incident.domain.ClassificationStatus;
import com.secopshub.incident.domain.Incident;
import com.secopshub.incident.domain.IncidentAuditEvent;
import com.secopshub.incident.domain.IncidentStatus;
import com.secopshub.incident.domain.Severity;
import com.secopshub.incident.error.ResourceNotFoundException;
import com.secopshub.incident.repository.IncidentAuditEventRepository;
import com.secopshub.incident.repository.IncidentRelationshipRepository;
import com.secopshub.incident.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentManagementServiceTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentRelationshipRepository relationshipRepository;
    @Mock private IncidentAuditEventRepository auditRepository;

    private IncidentManagementService service;

    @BeforeEach
    void setUp() {
        service = new IncidentManagementService(incidentRepository, relationshipRepository, auditRepository);
    }

    @Test
    void createNormalizesFieldsAndWritesAuditEvent() {
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setIncidentId("SEC-2026-UNIT0001");
            return incident;
        });

        IncidentResponse response = service.create(new CreateIncidentRequest(
                "  Suspicious login  ", "  Several failed attempts  ", " unauthorized_access ",
                " access_control ", Severity.HIGH, null, " analyst@example.test ",
                "  tier2@example.test ", " IT ", " IAM-STD-4 ", null, null,
                LocalDateTime.of(2026, 10, 17, 10, 0), " Potential account compromise ", null,
                " Reset credentials "));

        assertEquals("SEC-2026-UNIT0001", response.incidentId());
        assertEquals("Suspicious login", response.title());
        assertEquals("UNAUTHORIZED_ACCESS", response.incidentType());
        assertEquals("ACCESS_CONTROL", response.natureOfViolation());
        assertEquals(Severity.HIGH, response.severity());
        assertEquals(IncidentStatus.NEW, response.status());
        assertEquals(ClassificationStatus.SUSPECTED, response.classificationStatus());
        assertEquals("analyst@example.test", response.reportedBy());
        assertEquals("tier2@example.test", response.assignedTo());

        ArgumentCaptor<IncidentAuditEvent> auditCaptor = ArgumentCaptor.forClass(IncidentAuditEvent.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertEquals("INCIDENT_CREATED", auditCaptor.getValue().getAction());
        assertEquals("analyst@example.test", auditCaptor.getValue().getPerformedBy());
    }

    @Test
    void createFromAlertReturnsExistingIncidentForDuplicateAlert() {
        Incident existing = new Incident();
        existing.setIncidentId("SEC-2026-EXISTING");
        existing.setSourceAlertId("ALT-123");
        when(incidentRepository.findBySourceAlertId("ALT-123")).thenReturn(Optional.of(existing));

        String incidentId = service.createFromAlert(alertEvent("ALT-123"));

        assertEquals("SEC-2026-EXISTING", incidentId);
        verify(incidentRepository, never()).save(any(Incident.class));
        verifyNoInteractions(auditRepository);
    }

    @Test
    void createFromAlertRejectsEventWithoutAlertId() {
        AlertReceivedEvent invalidEvent = new AlertReceivedEvent(
                " ", "external-123", "SIEM", "Suspicious sign-in", "Description", "UNAUTHORIZED_ACCESS",
                "ACCESS_CONTROL", "HIGH", "SUSPECTED", "siem", null, null, null,
                null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.createFromAlert(invalidEvent));
        verifyNoInteractions(incidentRepository, auditRepository);
    }

    @Test
    void updateStatusAllowsNewToTriagedAndAuditsTransition() {
        Incident incident = incident("SEC-2026-STATUS01", IncidentStatus.NEW, Severity.HIGH, "ACCESS_CONTROL");
        when(incidentRepository.findByIncidentId("SEC-2026-STATUS01")).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncidentResponse response = service.updateStatus("SEC-2026-STATUS01",
                new UpdateStatusRequest(IncidentStatus.TRIAGED, "manager@example.test", "Analyst assigned"));

        assertEquals(IncidentStatus.TRIAGED, response.status());
        verify(auditRepository).save(argThat(event ->
                "STATUS_CHANGED".equals(event.getAction())
                        && "NEW".equals(event.getOldValue())
                        && "TRIAGED".equals(event.getNewValue())));
    }

    @Test
    void updateStatusRejectsInvalidTransition() {
        Incident incident = incident("SEC-2026-STATUS02", IncidentStatus.NEW, Severity.MEDIUM, "PHYSICAL_SECURITY");
        when(incidentRepository.findByIncidentId("SEC-2026-STATUS02")).thenReturn(Optional.of(incident));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus("SEC-2026-STATUS02",
                        new UpdateStatusRequest(IncidentStatus.CLOSED, "manager@example.test", "Close it")));

        assertTrue(error.getMessage().contains("not allowed"));
        verify(incidentRepository, never()).save(any(Incident.class));
        verifyNoInteractions(auditRepository);
    }

    @Test
    void quarterlyReportAggregatesSeverityStatusAndViolationCounts() {
        when(incidentRepository.findByFiscalYearAndReportingQuarterOrderByReportedAtDesc("FY2026-27", "Q3"))
                .thenReturn(List.of(
                        incident("SEC-2026-REPORT01", IncidentStatus.NEW, Severity.HIGH, "ACCESS_CONTROL"),
                        incident("SEC-2026-REPORT02", IncidentStatus.INVESTIGATING, Severity.HIGH, "ACCESS_CONTROL"),
                        incident("SEC-2026-REPORT03", IncidentStatus.TRIAGED, Severity.MEDIUM, "PHYSICAL_SECURITY")));

        QuarterlyReportResponse report = service.quarterlyReport(" fy2026-27 ", "q3");

        assertEquals("FY2026-27", report.fiscalYear());
        assertEquals("Q3", report.quarter());
        assertEquals(3, report.totalIncidents());
        assertEquals(2L, report.countsBySeverity().get("HIGH"));
        assertEquals(1L, report.countsBySeverity().get("MEDIUM"));
        assertEquals(1L, report.countsByStatus().get("INVESTIGATING"));
        assertEquals(2L, report.countsByViolation().get("ACCESS_CONTROL"));
    }

    @Test
    void quarterlyReportRejectsInvalidFiscalYear() {
        assertThrows(IllegalArgumentException.class, () -> service.quarterlyReport("2026/27", "Q3"));
        verify(incidentRepository, never()).findByFiscalYearAndReportingQuarterOrderByReportedAtDesc(anyString(), anyString());
    }

    @Test
    void getThrowsWhenIncidentDoesNotExist() {
        when(incidentRepository.findByIncidentId("SEC-DOES-NOT-EXIST")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.get("SEC-DOES-NOT-EXIST"));
    }

    private static Incident incident(String id, IncidentStatus status, Severity severity, String violation) {
        Incident incident = new Incident();
        incident.setIncidentId(id);
        incident.setTitle("Test incident");
        incident.setDescription("Test description");
        incident.setIncidentType("TEST_EVENT");
        incident.setNatureOfViolation(violation);
        incident.setSeverity(severity);
        incident.setStatus(status);
        incident.setClassificationStatus(ClassificationStatus.SUSPECTED);
        incident.setReportedBy("test@example.test");
        incident.setReportedAt(LocalDateTime.of(2026, 10, 1, 10, 0));
        return incident;
    }

    private static AlertReceivedEvent alertEvent(String alertId) {
        return new AlertReceivedEvent(alertId, "external-" + alertId, "TEST_SIEM", "Suspicious sign-in",
                "Several failed sign-ins followed by a success", "UNAUTHORIZED_ACCESS", "ACCESS_CONTROL",
                "HIGH", "SUSPECTED", "test-siem", "tier2@example.test", "IT", "IAM-STD-4",
                LocalDateTime.of(2026, 10, 17, 9, 0), LocalDateTime.of(2026, 10, 17, 9, 5),
                LocalDateTime.of(2026, 10, 17, 9, 10), "Potential account compromise", null, "Review sign-in logs");
    }
}

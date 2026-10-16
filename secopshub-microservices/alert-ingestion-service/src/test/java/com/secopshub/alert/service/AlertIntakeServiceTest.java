package com.secopshub.alert.service;

import com.secopshub.alert.domain.AlertProcessingStatus;
import com.secopshub.alert.domain.SecurityAlert;
import com.secopshub.alert.dto.AlertSubmissionResult;
import com.secopshub.alert.dto.SubmitAlertRequest;
import com.secopshub.alert.repository.SecurityAlertRepository;
import com.secopshub.contracts.AlertReceivedEvent;
import com.secopshub.contracts.MessagingNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertIntakeServiceTest {
    @Mock private SecurityAlertRepository repository;
    @Mock private RabbitTemplate rabbitTemplate;

    private AlertIntakeService service;

    @BeforeEach
    void setUp() {
        service = new AlertIntakeService(repository, rabbitTemplate);
    }

    @Test
    void submitPersistsAlertAndPublishesEvent() {
        when(repository.findByExternalAlertId("EDR-0001")).thenReturn(Optional.empty());
        when(repository.save(any(SecurityAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertSubmissionResult result = service.submit(request("EDR-0001"));

        assertTrue(result.publishSucceeded());
        assertTrue(result.acceptedOrRetried());
        assertEquals(AlertProcessingStatus.QUEUED.name(), result.alert().status());
        assertEquals("EDR-0001", result.alert().externalAlertId());

        ArgumentCaptor<AlertReceivedEvent> eventCaptor = ArgumentCaptor.forClass(AlertReceivedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(MessagingNames.ALERT_EXCHANGE),
                eq(MessagingNames.ALERT_ROUTING_KEY), eventCaptor.capture());
        assertEquals("EDR-0001", eventCaptor.getValue().externalAlertId());
        assertEquals("ENDPOINT_DETECTION", eventCaptor.getValue().source());
        assertEquals("UNAUTHORIZED_ACCESS", eventCaptor.getValue().incidentType());
        verify(repository, times(2)).save(any(SecurityAlert.class));
    }

    @Test
    void submitDoesNotRepublishAlertAlreadyQueued() {
        SecurityAlert existing = alert("EDR-0002", AlertProcessingStatus.QUEUED);
        when(repository.findByExternalAlertId("EDR-0002")).thenReturn(Optional.of(existing));

        AlertSubmissionResult result = service.submit(request("EDR-0002"));

        assertFalse(result.acceptedOrRetried());
        assertTrue(result.publishSucceeded());
        assertEquals(AlertProcessingStatus.QUEUED.name(), result.alert().status());
        verify(repository, never()).save(any(SecurityAlert.class));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void submitMarksAlertPublishFailedWhenBrokerIsUnavailable() {
        when(repository.findByExternalAlertId("EDR-0003")).thenReturn(Optional.empty());
        when(repository.save(any(SecurityAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new AmqpException("broker unavailable")).when(rabbitTemplate)
                .convertAndSend(eq(MessagingNames.ALERT_EXCHANGE), eq(MessagingNames.ALERT_ROUTING_KEY), any(AlertReceivedEvent.class));

        AlertSubmissionResult result = service.submit(request("EDR-0003"));

        assertFalse(result.publishSucceeded());
        assertEquals(AlertProcessingStatus.PUBLISH_FAILED.name(), result.alert().status());
        assertNotNull(result.alert().lastError());
        assertTrue(result.alert().lastError().contains("broker unavailable"));
        verify(repository, times(3)).save(any(SecurityAlert.class));
    }

    @Test
    void markIncidentCreatedUpdatesStatusAndIncidentReference() {
        SecurityAlert existing = alert("EDR-0004", AlertProcessingStatus.QUEUED);
        when(repository.findByAlertId("ALT-UNIT-0004")).thenReturn(Optional.of(existing));
        LocalDateTime processedAt = LocalDateTime.of(2026, 10, 17, 11, 0);

        service.markIncidentCreated("ALT-UNIT-0004", "SEC-2026-0004", processedAt);

        assertEquals(AlertProcessingStatus.INCIDENT_CREATED, existing.getStatus());
        assertEquals("SEC-2026-0004", existing.getCreatedIncidentId());
        assertEquals(processedAt, existing.getProcessedAt());
        assertNull(existing.getLastError());
        verify(repository).save(existing);
    }

    private static SubmitAlertRequest request(String externalAlertId) {
        return new SubmitAlertRequest(externalAlertId, "ENDPOINT_DETECTION", "Suspicious sign-in",
                "Repeated failed logins followed by success", "UNAUTHORIZED_ACCESS", "ACCESS_CONTROL",
                "HIGH", "SUSPECTED", "edr-connector@example.test", "tier2@example.test", "IT",
                "IAM-STD-4", LocalDateTime.of(2026, 10, 17, 9, 0),
                LocalDateTime.of(2026, 10, 17, 9, 5), LocalDateTime.of(2026, 10, 17, 9, 10),
                "Potential unauthorized access", null, "Review sign-in logs and enforce MFA");
    }

    private static SecurityAlert alert(String externalAlertId, AlertProcessingStatus status) {
        SecurityAlert alert = new SecurityAlert();
        alert.setExternalAlertId(externalAlertId);
        alert.setSource("ENDPOINT_DETECTION");
        alert.setTitle("Suspicious sign-in");
        alert.setIncidentType("UNAUTHORIZED_ACCESS");
        alert.setNatureOfViolation("ACCESS_CONTROL");
        alert.setSeverity("HIGH");
        alert.setStatus(status);
        alert.setReportedAt(LocalDateTime.of(2026, 10, 17, 9, 10));
        return alert;
    }
}

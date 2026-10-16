package com.secopshub.incident.messaging;

import com.secopshub.contracts.AlertReceivedEvent;
import com.secopshub.contracts.IncidentCreatedForAlertEvent;
import com.secopshub.contracts.MessagingNames;
import com.secopshub.incident.service.IncidentManagementService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class IncidentAlertEventListener {
    private final IncidentManagementService incidentManagementService;
    private final RabbitTemplate rabbitTemplate;

    public IncidentAlertEventListener(IncidentManagementService incidentManagementService, RabbitTemplate rabbitTemplate) {
        this.incidentManagementService = incidentManagementService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = MessagingNames.INCIDENT_ALERT_QUEUE)
    public void onAlertReceived(AlertReceivedEvent event) {
        String incidentId = incidentManagementService.createFromAlert(event);
        rabbitTemplate.convertAndSend(MessagingNames.ALERT_STATUS_EXCHANGE, MessagingNames.ALERT_STATUS_ROUTING_KEY,
                new IncidentCreatedForAlertEvent(event.alertId(), event.externalAlertId(), incidentId, LocalDateTime.now()));
    }
}

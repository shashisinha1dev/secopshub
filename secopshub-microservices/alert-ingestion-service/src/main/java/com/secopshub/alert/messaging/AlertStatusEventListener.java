package com.secopshub.alert.messaging;

import com.secopshub.alert.service.AlertIntakeService;
import com.secopshub.contracts.IncidentCreatedForAlertEvent;
import com.secopshub.contracts.MessagingNames;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AlertStatusEventListener {
    private final AlertIntakeService alertIntakeService;

    public AlertStatusEventListener(AlertIntakeService alertIntakeService) {
        this.alertIntakeService = alertIntakeService;
    }

    @RabbitListener(queues = MessagingNames.ALERT_STATUS_QUEUE)
    public void onIncidentCreated(IncidentCreatedForAlertEvent event) {
        alertIntakeService.markIncidentCreated(event.alertId(), event.incidentId(), event.processedAt());
    }
}

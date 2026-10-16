package com.secopshub.contracts;

public final class MessagingNames {
    private MessagingNames() { }

    public static final String ALERT_EXCHANGE = "secopshub.alerts";
    public static final String ALERT_ROUTING_KEY = "alert.received";
    public static final String INCIDENT_ALERT_QUEUE = "secopshub.incident.alert.received";
    public static final String ALERT_DEAD_LETTER_EXCHANGE = "secopshub.alerts.dlx";
    public static final String ALERT_DEAD_LETTER_ROUTING_KEY = "alert.received.dead";
    public static final String INCIDENT_ALERT_DLQ = "secopshub.incident.alert.received.dlq";

    public static final String ALERT_STATUS_EXCHANGE = "secopshub.alert.status";
    public static final String ALERT_STATUS_ROUTING_KEY = "incident.created";
    public static final String ALERT_STATUS_QUEUE = "secopshub.alert.status.update";
}

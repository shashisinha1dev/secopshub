package com.secopshub.contracts;

import java.time.LocalDateTime;

/** Acknowledgement event that updates the originating alert's processing status. */
public record IncidentCreatedForAlertEvent(
        String alertId,
        String externalAlertId,
        String incidentId,
        LocalDateTime processedAt
) { }

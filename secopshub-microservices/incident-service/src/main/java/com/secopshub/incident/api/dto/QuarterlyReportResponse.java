package com.secopshub.incident.api.dto;

import java.util.Map;

public record QuarterlyReportResponse(String fiscalYear, String quarter, long totalIncidents,
                                      Map<String, Long> countsBySeverity,
                                      Map<String, Long> countsByStatus,
                                      Map<String, Long> countsByViolation) { }

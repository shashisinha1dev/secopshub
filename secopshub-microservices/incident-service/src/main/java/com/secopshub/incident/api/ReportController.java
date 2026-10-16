package com.secopshub.incident.api;

import com.secopshub.incident.api.dto.QuarterlyReportResponse;
import com.secopshub.incident.service.IncidentManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final IncidentManagementService service;
    public ReportController(IncidentManagementService service) { this.service = service; }

    @GetMapping("/quarterly")
    public QuarterlyReportResponse quarterly(@RequestParam(required = false) String fiscalYear,
                                             @RequestParam(required = false) String quarter) {
        return service.quarterlyReport(fiscalYear, quarter);
    }
}

package com.secopshub.incident.api;

import com.secopshub.incident.api.dto.*;
import com.secopshub.incident.domain.IncidentStatus;
import com.secopshub.incident.domain.RelationshipType;
import com.secopshub.incident.domain.Severity;
import com.secopshub.incident.service.IncidentManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {
    private final IncidentManagementService service;
    public IncidentController(IncidentManagementService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse create(@Valid @RequestBody CreateIncidentRequest request) { return service.create(request); }

    @GetMapping("/{incidentId}")
    public IncidentResponse get(@PathVariable String incidentId) { return service.get(incidentId); }

    @GetMapping
    public Page<IncidentResponse> search(
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) String natureOfViolation,
            @RequestParam(required = false) String incidentType,
            @RequestParam(required = false) String fiscalYear,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(severity, status, natureOfViolation, incidentType, fiscalYear, quarter, assignedTo, PageRequest.of(page, size));
    }

    @PatchMapping("/{incidentId}/severity")
    public IncidentResponse updateSeverity(@PathVariable String incidentId, @Valid @RequestBody UpdateSeverityRequest request) {
        return service.updateSeverity(incidentId, request);
    }

    @PatchMapping("/{incidentId}/status")
    public IncidentResponse updateStatus(@PathVariable String incidentId, @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(incidentId, request);
    }

    @PostMapping("/{incidentId}/relationships")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentRelationshipResponse addRelationship(@PathVariable String incidentId, @Valid @RequestBody CreateRelationshipRequest request) {
        return service.addRelationship(incidentId, request);
    }

    @GetMapping("/{incidentId}/relationships")
    public List<IncidentRelationshipResponse> relationships(@PathVariable String incidentId) { return service.relationships(incidentId); }

    @GetMapping("/{incidentId}/audit-events")
    public List<AuditEventResponse> auditEvents(@PathVariable String incidentId) { return service.auditEvents(incidentId); }
}

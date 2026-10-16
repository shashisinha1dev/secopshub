package com.secopshub.alert.api;

import com.secopshub.alert.dto.AlertResponse;
import com.secopshub.alert.dto.AlertSubmissionResult;
import com.secopshub.alert.dto.SubmitAlertRequest;
import com.secopshub.alert.service.AlertIntakeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {
    private final AlertIntakeService service;

    public AlertController(AlertIntakeService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<AlertResponse> submit(@Valid @RequestBody SubmitAlertRequest request) {
        AlertSubmissionResult result = service.submit(request);
        if (!result.publishSucceeded()) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result.alert());
        if (!result.acceptedOrRetried()) return ResponseEntity.ok(result.alert());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result.alert());
    }

    @GetMapping("/{externalAlertId}")
    public AlertResponse get(@PathVariable String externalAlertId) {
        return service.getByExternalAlertId(externalAlertId);
    }

    @GetMapping
    public List<AlertResponse> list() { return service.list(); }
}

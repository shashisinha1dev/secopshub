package com.secopshub.incident.api;

import com.secopshub.incident.api.dto.ViolationCategoryResponse;
import com.secopshub.incident.domain.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reference")
public class ReferenceController {
    @GetMapping("/violation-categories")
    public List<ViolationCategoryResponse> violationCategories() {
        return List.of(
                new ViolationCategoryResponse("ACCESS_CONTROL", "Access Control", "Unauthorized access, excessive privileges, credential sharing."),
                new ViolationCategoryResponse("DATA_PROTECTION", "Data Protection", "Data leakage, improper handling, unauthorized disclosure."),
                new ViolationCategoryResponse("PHYSICAL_SECURITY", "Physical Security", "Tailgating, unauthorized entry, lost access badges."),
                new ViolationCategoryResponse("MALWARE_ENDPOINT", "Malware and Endpoint", "Malware infection, unauthorized software, compromised devices."),
                new ViolationCategoryResponse("ACCEPTABLE_USE", "Acceptable Use", "Misuse of corporate systems or prohibited applications."),
                new ViolationCategoryResponse("CHANGE_MANAGEMENT", "Change Management", "Unauthorized changes or bypassed approvals."),
                new ViolationCategoryResponse("THIRD_PARTY_SECURITY", "Third-Party Security", "Supplier or vendor security violations."),
                new ViolationCategoryResponse("PRIVACY_COMPLIANCE", "Privacy and Compliance", "Suspected privacy violation or non-compliance."));
    }

    @GetMapping("/severities")
    public List<String> severities() { return Arrays.stream(Severity.values()).map(Enum::name).toList(); }
    @GetMapping("/statuses")
    public List<String> statuses() { return Arrays.stream(IncidentStatus.values()).map(Enum::name).toList(); }
    @GetMapping("/classification-statuses")
    public List<String> classifications() { return Arrays.stream(ClassificationStatus.values()).map(Enum::name).toList(); }
    @GetMapping("/relationship-types")
    public List<String> relationshipTypes() { return Arrays.stream(RelationshipType.values()).map(Enum::name).toList(); }
}

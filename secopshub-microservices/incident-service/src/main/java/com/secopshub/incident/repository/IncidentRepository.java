package com.secopshub.incident.repository;

import com.secopshub.incident.domain.Incident;
import com.secopshub.incident.domain.IncidentStatus;
import com.secopshub.incident.domain.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Optional<Incident> findByIncidentId(String incidentId);
    Optional<Incident> findBySourceAlertId(String sourceAlertId);

    @Query("""
        select i from Incident i
        where (:severity is null or i.severity = :severity)
          and (:status is null or i.status = :status)
          and (:natureOfViolation is null or lower(i.natureOfViolation) = lower(:natureOfViolation))
          and (:incidentType is null or lower(i.incidentType) = lower(:incidentType))
          and (:fiscalYear is null or i.fiscalYear = :fiscalYear)
          and (:quarter is null or i.reportingQuarter = :quarter)
          and (:assignedTo is null or lower(i.assignedTo) = lower(:assignedTo))
        order by i.reportedAt desc
        """)
    Page<Incident> search(@Param("severity") Severity severity,
                          @Param("status") IncidentStatus status,
                          @Param("natureOfViolation") String natureOfViolation,
                          @Param("incidentType") String incidentType,
                          @Param("fiscalYear") String fiscalYear,
                          @Param("quarter") String quarter,
                          @Param("assignedTo") String assignedTo,
                          Pageable pageable);

    List<Incident> findByFiscalYearAndReportingQuarterOrderByReportedAtDesc(String fiscalYear, String reportingQuarter);
}

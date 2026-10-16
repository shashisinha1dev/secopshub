package com.secopshub.incident.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportingPeriodTest {
    @Test
    void aprilStartsNewFiscalYearAndQ1() {
        ReportingPeriod period = ReportingPeriod.from(LocalDateTime.of(2026, 4, 1, 0, 0));
        assertEquals("FY2026-27", period.fiscalYear());
        assertEquals("Q1", period.quarter());
    }

    @Test
    void octoberBelongsToQ3OfCurrentFiscalYear() {
        ReportingPeriod period = ReportingPeriod.from(LocalDateTime.of(2026, 10, 17, 12, 0));
        assertEquals("FY2026-27", period.fiscalYear());
        assertEquals("Q3", period.quarter());
    }

    @Test
    void marchBelongsToQ4OfPreviousFiscalYear() {
        ReportingPeriod period = ReportingPeriod.from(LocalDateTime.of(2027, 3, 31, 23, 59));
        assertEquals("FY2026-27", period.fiscalYear());
        assertEquals("Q4", period.quarter());
    }
}

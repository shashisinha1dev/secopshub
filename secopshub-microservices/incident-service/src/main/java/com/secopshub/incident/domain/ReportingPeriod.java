package com.secopshub.incident.domain;

import java.time.LocalDateTime;
import java.util.Locale;

public record ReportingPeriod(String fiscalYear, String quarter) {
    public static ReportingPeriod from(LocalDateTime dateTime) {
        int month = dateTime.getMonthValue();
        int startingYear = month >= 4 ? dateTime.getYear() : dateTime.getYear() - 1;
        int quarterNumber = (month >= 4 && month <= 6) ? 1
                : (month >= 7 && month <= 9) ? 2
                : (month >= 10) ? 3 : 4;
        return new ReportingPeriod(
                String.format(Locale.ROOT, "FY%d-%02d", startingYear, (startingYear + 1) % 100),
                "Q" + quarterNumber);
    }
}

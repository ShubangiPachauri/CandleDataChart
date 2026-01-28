package com.CandleData.service.HistoricalData;

import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
@Component
public class PartitionPlanner {

    private static final DateTimeFormatter NAME_FMT =
            DateTimeFormatter.ofPattern("yyyy_MM");

    // Example: p2025_03
    public String nextPartitionName() {
        return "p" + YearMonth.now().plusMonths(1).format(NAME_FMT);
    }

    // Example: TO_DAYS('2025-04-01')
    public String nextPartitionLessThan() {
        YearMonth nextNext = YearMonth.now().plusMonths(2);
        return "TO_DAYS('" + nextNext.atDay(1) + "')";
    }
}

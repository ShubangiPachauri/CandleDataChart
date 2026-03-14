package com.CandleData.service.util;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.CandleData.entity.HistoricalData.SyncTracker;

@Service
public class AppUtils {

    @Value("${historical.sync.month}")
    private String syncMonth;

    private static final SimpleDateFormat FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Date determineStartDate() {

        YearMonth ym = YearMonth.parse(syncMonth);

        LocalDate firstDay = ym.atDay(1);

        String start = firstDay + " 09:15:00";

        try {
            return FORMAT.parse(start);
        } catch (Exception e) {
            throw new RuntimeException("Invalid start date", e);
        }
    }

    public Date determineToDate() {

        YearMonth ym = YearMonth.parse(syncMonth);

        LocalDate lastDay = ym.atEndOfMonth();

        String end = lastDay + " 15:30:00";

        try {
            return FORMAT.parse(end);
        } catch (Exception e) {
            throw new RuntimeException("Invalid end date", e);
        }
    }

    public boolean isAlreadySyncedToday(SyncTracker tracker) {

        return tracker.getLastRunAt() != null &&
                tracker.getLastRunAt().toLocalDate().equals(LocalDate.now()) &&
                "SUCCESS".equals(tracker.getStatus());
    }
}
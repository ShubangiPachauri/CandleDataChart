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

	 @Value("${historical.sync.range.minute}")
	    private int minuteRange;

	    @Value("${historical.sync.range.5minute}")
	    private int fiveMinuteRange;

	    @Value("${historical.sync.range.default}")
	    private int defaultRange;

	    private static final SimpleDateFormat FORMAT =
	            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    
    public Date determineStartDate(String interval) {

        int days;

        switch (interval) {

            case "minute":
                days = minuteRange;
                break;

            case "5minute":
                days = fiveMinuteRange;
                break;

            default:
                days = defaultRange;
        }

        LocalDate startDate = LocalDate.now().minusDays(days);

        String start = startDate + " 09:15:00";

        try {
            return FORMAT.parse(start);
        } catch (Exception e) {
            throw new RuntimeException("Invalid start date", e);
        }
    }


    public Date determineToDate() {

        LocalDate today = LocalDate.now();

        String end = today + " 15:30:00";

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
package com.CandleData.service.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class HistoricalTableNameUtil {

    private static final DateTimeFormatter KITE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

    private HistoricalTableNameUtil() {}

    public static String resolveTableName(String interval, String candleTimestamp) {

        OffsetDateTime odt = OffsetDateTime.parse(candleTimestamp, KITE_FORMAT);

        String month = odt.format(MONTH_FORMAT).toLowerCase();
        int year = odt.getYear();

        return interval + "_historicaldata_eq_" + month + "_" + year;
    }
}

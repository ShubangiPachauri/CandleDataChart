package com.CandleData.service;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AppConstant {

    public static final String MARKET_OPEN_TIME = " 09:15:00";

    public static final String MINUTE   = "minute";
    public static final String FIVE_MINUTE   = "5minute";
    public static final String FIFTEEN_MINUTE = "15minute";
    public static final String SIXTY_MINUTE  = "60minute";

    public static final String DAY  = "day";
    public static final String WEEK = "week";
    
    public static final int DB_BATCH_SIZE = 12000;
}

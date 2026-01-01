package com.CandleData.entity.HistoricalData;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalData {
    private String id; // format: instrumentToken_timeStamp
    private String timeStamp;
    private String tradingSymbol;
    private Long instrumentToken;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    private long oi;
}
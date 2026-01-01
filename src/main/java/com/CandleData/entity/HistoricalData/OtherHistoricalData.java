package com.CandleData.entity.HistoricalData;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherHistoricalData {
    @Id
    private String id; // instrumentToken + timestamp + timeframe
    private String timeStamp;
    private String tradingSymbol;
    private long instrumentToken;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    private long oi;
    private String timeframe; 
}
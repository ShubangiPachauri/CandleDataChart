//package com.CandleData.entity.HistoricalData;
//
//import jakarta.persistence.Column;
//import lombok.*;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class HistoricalData {
//    private String id;
//    
//    @Column(name = "time_stamp")
//    private String timeStamp;
//    
//    @Column(name = "trading_symbol")
//    private String tradingSymbol;
//    
//    @Column(name = "instrument_token")
//    private Long instrumentToken;
//    
//    @Column(name = "open_price")
//    private Double open;
//
//    @Column(name = "high_price")
//    private Double high;
//
//    @Column(name = "low_price")
//    private Double low;
//
//    @Column(name = "close_price")
//    private Double close;
//    private long volume;
//    private long oi;
//}
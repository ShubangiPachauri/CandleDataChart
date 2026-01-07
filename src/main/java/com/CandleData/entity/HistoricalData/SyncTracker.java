package com.CandleData.entity.HistoricalData;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sync_tracker")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncTracker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String tradingSymbol;
    private Long instrumentToken;
    
    @Column(name = "`interval` ") 
    private String interval;// 5minute, 15minute, etc.
    
    private String lastFetchedTimestamp; // Last successful candle time
    private String status; // SUCCESS, FAILED, IN_PROGRESS
    private LocalDateTime lastRunAt;
}
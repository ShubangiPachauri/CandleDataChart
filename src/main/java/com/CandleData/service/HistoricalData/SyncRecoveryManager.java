package com.CandleData.service.HistoricalData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.CandleData.scheduler.HistoricalData.HistoricalDataScheduler;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

@Component
@Slf4j
@RequiredArgsConstructor
public class SyncRecoveryManager {

    private final HistoricalDataScheduler scheduler;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStart() throws KiteException {
        try {
            // Wait for DB connections and Token to be ready
            log.info("Startup Recovery: Waiting 15 seconds for system to stabilize...");
            Thread.sleep(15000); 
            
            log.info("Starting missed data recovery check...");
            scheduler.runDailySync();
        } catch (Exception e) {
            log.error("Recovery process interrupted: {}", e.getMessage());
        }
    }
}
package com.CandleData.controller.HistoricalData;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CandleData.service.HistoricalData.HistoricalData15MinService;
import com.CandleData.service.HistoricalData.HistoricalData5MinService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import java.util.Map;

@RestController
@RequestMapping("/api/historical")
@RequiredArgsConstructor
public class HistoricalDataController {

    private final HistoricalData5MinService historicalDataService;
    private final HistoricalData15MinService historicalData15MinService;
    
    @GetMapping("/sync/{month}/{year}")
    public ResponseEntity<?> triggerSync(@PathVariable String month, @PathVariable String year) throws KiteException {
        try {
            historicalDataService.syncMonthlyData(month, year);
            return ResponseEntity.ok(Map.of("message", "5 Minute Sync started for " + month + "_" + year));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
    
    @GetMapping("/sync/15min/{month}/{year}")
    public ResponseEntity<?> trigger15MinSync(@PathVariable String month, @PathVariable String year) throws KiteException {
        try {
            historicalData15MinService.syncMonthlyData15Min(month, year);
            return ResponseEntity.ok(Map.of("message", "15 Minute Sync started for " + month + "_" + year));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
package com.CandleData.controller.HistoricalData;

import com.CandleData.entity.HistoricalData.OtherHistoricalData;
import com.CandleData.service.HistoricalData.OtherHistoricalDataService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/historical/other")
@RequiredArgsConstructor
public class OtherHistoricalDataController {

    private final OtherHistoricalDataService otherHistoricalDataService;

    @GetMapping("/sync/{timeframe}/{month}/{year}")
    public ResponseEntity<?> triggerSync(@PathVariable String timeframe, @PathVariable String month, @PathVariable String year) throws KiteException {
        if (!isValidTimeframe(timeframe)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid timeframe. Use 60minute, day, or 7day"));
        }
        otherHistoricalDataService.syncData(month, year, timeframe);
        return ResponseEntity.ok(Map.of("message", "Sync started for " + timeframe + " in table OtherHistoricalData_EQ_" + month + "_" + year));
    }

    
    @GetMapping("/fetch-data")
    public ResponseEntity<?> getData(@RequestParam String instrumentToken, @RequestParam String fromDate, @RequestParam String toDate, @RequestParam String timeframe) {
        List<OtherHistoricalData> data = otherHistoricalDataService.getHistoricalDataFromDB(instrumentToken, fromDate, toDate, timeframe);
        return data.isEmpty() ? ResponseEntity.status(404).body(Map.of("message", "No data found")) : ResponseEntity.ok(data);
    }

    private boolean isValidTimeframe(String tf) {
        return tf.equals("60minute") || tf.equals("day") || tf.equals("7day");
    }
}
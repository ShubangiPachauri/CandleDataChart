package com.CandleData.controller.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.CandleData.service.stock.StockService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    @GetMapping("/sync")
    public ResponseEntity<?> syncStocks() throws KiteException {
        try {
            stockService.syncAllInstruments();
            return ResponseEntity.ok(Map.of(
                "status", "Success",
                "message", "Instruments synced and saved to database"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "Error",
                "message", e.getMessage()
            ));
        }
    }
}
package com.CandleData.controller.stock;

import com.CandleData.service.stock.StockService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
@Slf4j
public class StockController {

    private final StockService stockService;

    @GetMapping("/sync-nifty500")
    public ResponseEntity<?> syncNifty500Stocks() {

        try {

            log.info("Manual Nifty500 sync triggered");

            stockService.syncNifty500Stocks();

            return ResponseEntity.ok(
                    Map.of(
                            "status", "SUCCESS",
                            "message", "Nifty 500 stocks synced successfully"
                    )
            );

        }

        catch (KiteException e) {

            log.error("Kite API error: {}", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    Map.of(
                            "status", "ERROR",
                            "message", "Kite API error: " + e.getMessage()
                    )
            );
        }

        catch (RuntimeException e) {

            log.error("Kite login required: {}", e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of(
                            "status", "ERROR",
                            "message", e.getMessage()
                    )
            );
        }

        catch (Exception e) {

            log.error("Stock sync failed", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of(
                            "status", "ERROR",
                            "message", "Stock sync failed"
                    )
            );
        }
    }
}
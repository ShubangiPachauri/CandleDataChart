package com.CandleData.controller.index;

import com.CandleData.service.index.IndexService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/indices")
@Slf4j
public class IndexController {

    private final IndexService indexService;

    @GetMapping("/sync")
    public ResponseEntity<?> syncIndices() throws KiteException {

        try {

            indexService.syncTopIndices();

            return ResponseEntity.ok(
                    Map.of(
                            "status", "SUCCESS",
                            "message", "Indices synced successfully"
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "status", "ERROR",
                            "message", e.getMessage()
                    )
            );
        }
    }
}
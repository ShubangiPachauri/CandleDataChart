package com.CandleData.controller.kite;

import com.CandleData.service.kite.KiteService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class KiteController {

    private final KiteService kiteService; // Injection fixed

    @GetMapping("/kite/login")
    public ResponseEntity<String> loginUrl() {
        return ResponseEntity.ok(kiteService.getLoginUrl());
    }

    @PostMapping("/access-token")
    public ResponseEntity<?> getAccessToken(@RequestParam String requestToken) {
        try {
            log.info("Generating session for token: {}", requestToken);
            kiteService.generateSession(requestToken);
            return ResponseEntity.ok(Map.of("message", "Login successful"));
            
        } catch (IOException | KiteException e) {
            log.error("Kite API Error: {}", e.getMessage());  
            return ResponseEntity.status(500).body("Kite Error: " + e.getMessage());
            
        } catch (Exception e) {
            log.error("System Error: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
}
package com.CandleData.scheduler;

import com.CandleData.service.stock.StockService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockScheduler {

    private final StockService stockService;

    @Scheduled(cron = "0 0 6 ? * MON-FRI")
    public void syncStocks() throws KiteException {

        try {

            stockService.syncNifty500Stocks();

        } catch (Exception e) {

            log.error("Stock sync failed {}", e.getMessage());
        }
    }
}
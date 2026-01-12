package com.CandleData.scheduler.HistoricalData;

import com.CandleData.entity.stock.Stock;
import com.CandleData.repository.stock.StockRepository;
import com.CandleData.service.HistoricalData.HistoricalDataProcessor;
import com.CandleData.service.kite.KiteService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.CandleData.repository.HistoricalData.HistoricalDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class HistoricalDataScheduler {

    private final StockRepository stockRepository;
    private final HistoricalDataProcessor processor;
    private final HistoricalDataRepository historicalRepository;
    private final KiteService kiteService;

    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void runDailySync() throws KiteException {
        if (!isTokenValid()) {           
            log.error("Kite token missing. Login required.");
            return;
        }

        log.info("Historical data sync started at {}", new Date());
        
        List<Stock> stocks = stockRepository.findAll()
                .stream()
                .filter(s ->
                        "TCS".equalsIgnoreCase(s.getTradingSymbol()) ||
                        "RELIANCE".equalsIgnoreCase(s.getTradingSymbol())
                )
                .toList();

        String[] intervals = {"5minute", "15minute", "60minute", "day", "week"};
        String monthYear = new SimpleDateFormat("MMM_yyyy").format(new Date()).toUpperCase();

        for (String interval : intervals) {
            log.info("Processing Interval: [{}]", interval);
            String tableName = interval + "_HistoricalData_EQ_" + monthYear;
            historicalRepository.createTableIfNotExist(tableName);
            
            log.info("Interval [{}] started", interval);

            for (int i = 0; i < stocks.size(); i++) {
                Stock stock = stocks.get(i);
                try {
                    // Har 50 stock ke baad ek progress update
                    if (i % 50 == 0) log.info("Progress: {}/{} stocks done for {}", i, stocks.size(), interval);
                    
                    processor.processStockData(stock, interval, tableName);
                    Thread.sleep(350); 
                } catch (Exception e) {
                    // Log already handled in processor
                }
            }
        }
        log.info("Historical data sync completed at {}", new Date());
    }
    private boolean isTokenValid() {
        try {
            return kiteService.getKiteConnect().getAccessToken() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
package com.CandleData.service.HistoricalData;

import com.CandleData.entity.HistoricalData.HistoricalData;
import com.CandleData.entity.HistoricalData.SyncTracker;
import com.CandleData.entity.stock.Stock;
import com.CandleData.repository.HistoricalData.HistoricalDataRepository;
import com.CandleData.repository.HistoricalData.SyncTrackerRepository;
import com.CandleData.service.kite.KiteService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricalDataProcessor {

    private final KiteService kiteService;
    private final HistoricalDataRepository historicalRepository;
    private final SyncTrackerRepository trackerRepository;

    @Transactional
    public void processStockData(Stock stock, String interval, String tableName) throws Exception, KiteException {
    	if (stock == null || stock.getInstrumentToken() == null || stock.getTradingSymbol() == null) {
            log.error(">>> [SKIP] Found null stock or token in database!");
            return;
        }
    	
        String symbol = stock.getTradingSymbol();
        String token = String.valueOf(stock.getInstrumentToken());
        
        SyncTracker tracker = trackerRepository
                .findByInstrumentTokenAndInterval(stock.getInstrumentToken(), interval)
                .orElse(SyncTracker.builder()
                        .tradingSymbol(symbol)
                        .instrumentToken(stock.getInstrumentToken())
                        .interval(interval)
                        .build());

        // 1. Skip if already done
        if (isAlreadySyncedToday(tracker)) {
            log.info(">>> [SKIP] {} ({}) already synced for today.", symbol, interval);
            return;
        }

        Date fromDate = determineStartDate(tracker);
        Date toDate = determineToDate();

        // 2. Logic Check: Market Hours Guard
        if (fromDate.after(toDate)) {
            log.info(">>> [AWAIT] {} ({}) - Previous data complete. Waiting for market close (15:30) for new data.", symbol, interval);
            return;
        }

        log.info(">>> [FETCHING] {} ({}) from {} to {}", symbol, interval, fromDate, toDate);

        try {
            List<com.zerodhatech.models.HistoricalData> kiteData = kiteService.getKiteConnect()
                    .getHistoricalData(fromDate, toDate, String.valueOf(stock.getInstrumentToken()), interval, false, true)
                    .dataArrayList;

            if (kiteData != null && !kiteData.isEmpty()) {
                List<HistoricalData> entities = mapToEntity(kiteData, stock);
                historicalRepository.saveBatch(tableName, entities);

                // Update Tracker
                tracker.setLastFetchedTimestamp(kiteData.get(kiteData.size() - 1).timeStamp);
                tracker.setStatus("SUCCESS");
                tracker.setLastRunAt(LocalDateTime.now());
                trackerRepository.save(tracker);
                
                log.info(">>> [SUCCESS] {} ({}): {} records saved to {}", symbol, interval, entities.size(), tableName);
            } else {
                log.warn(">>> [NO DATA] No new data found for {} ({}) in given range.", symbol, interval);
            }
        } catch (Exception e) {
            log.error(">>> [ERROR] Failed to fetch data for {} ({}): {}", symbol, interval, e.getMessage());
            throw e; // Rethrow for scheduler catch block
        }
    }

    private Date determineToDate() {
        LocalTime now = LocalTime.now();
        // Agar abhi 3:30 PM (15:30) nahi baje hain
        if (now.isBefore(LocalTime.of(15, 30))) {
            // Toh hum sirf KAL sham 3:30 tak ka data mang sakte hain
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DATE, -1); // Kal ki date
            cal.set(java.util.Calendar.HOUR_OF_DAY, 15);
            cal.set(java.util.Calendar.MINUTE, 30);
            return cal.getTime();
        } else {
            // Agar 3:30 PM beet chuke hain, toh abhi (current time) tak ka data mang lo
            return new Date();
        }
    }

    private boolean isAlreadySyncedToday(SyncTracker tracker) {
        return tracker.getLastRunAt() != null && 
               tracker.getLastRunAt().toLocalDate().equals(LocalDate.now()) && 
               "SUCCESS".equals(tracker.getStatus());
    }

    private Date determineStartDate(SyncTracker tracker) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (tracker.getLastFetchedTimestamp() != null) {
        	Date lastDate = sdf.parse(tracker.getLastFetchedTimestamp());
        	return new Date(lastDate.getTime() + 1000);
        }
        // Agar ekdum naya stock hai, toh aaj subah 9:15 se start
        String todayStart = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 09:15:00";
        return sdf.parse(todayStart);
    }

    private List<HistoricalData> mapToEntity(List<com.zerodhatech.models.HistoricalData> list, Stock stock) {
        return list.stream().map(d -> HistoricalData.builder()
                .id(stock.getInstrumentToken() + "_" + d.timeStamp)
                .timeStamp(d.timeStamp)
                .tradingSymbol(stock.getTradingSymbol())
                .instrumentToken(stock.getInstrumentToken())
                .open(d.open).high(d.high).low(d.low).close(d.close)
                .volume(d.volume).oi(d.oi).build()
        ).collect(Collectors.toList());  
    }
}
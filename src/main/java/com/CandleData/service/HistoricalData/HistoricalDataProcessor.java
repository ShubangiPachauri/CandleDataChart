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
import org.springframework.util.ObjectUtils;

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
    public void processStockData(Stock stock, String interval, String tableName, SyncTracker tracker) throws Exception, KiteException {
    	if (stock == null || stock.getInstrumentToken() == null || stock.getTradingSymbol() == null) {
            log.error("[SKIP] Found null stock or token in database!");
            return;
        }
    	
        String symbol = stock.getTradingSymbol();
        //String token = String.valueOf(stock.getInstrumentToken());
        if(ObjectUtils.isEmpty(tracker) ) {
        	tracker = SyncTracker.builder()
	          .tradingSymbol(symbol)
	          .instrumentToken(stock.getInstrumentToken())
	          .interval(interval)
	          .build();
        }
//        SyncTracker tracker = trackerRepository
//                .findByInstrumentTokenAndInterval(stock.getInstrumentToken(), interval)
//                .orElse(SyncTracker.builder()
//                        .tradingSymbol(symbol)
//                        .instrumentToken(stock.getInstrumentToken())
//                        .interval(interval)
//                        .build());

        // 1. Skip if already done
        if (isAlreadySyncedToday(tracker)) {
            log.info(">>> [SKIP] {} ({}) already synced for today.", symbol, interval);
            return;
        }

        Date fromDate = determineStartDate(tracker, interval);
        Date toDate = determineToDate();
        
     // FIX: DAY / WEEK / MONTH interval ke liye TIME strip karna
        if ("day".equals(interval) || "week".equals(interval) || "month".equals(interval)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            fromDate = sdf.parse(sdf.format(fromDate));
            toDate   = sdf.parse(sdf.format(toDate));
        }


        // 2. Logic Check: Market Hours Guard
        if (fromDate.after(toDate)) {
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
            } 
        } catch (Exception e) {
            log.error(">>> [ERROR] Failed to fetch data for {} ({}): {}", symbol, interval, e.getMessage());
            throw e; // Rethrow for scheduler catch block
        }
    }

    private Date determineToDate() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(15, 30))) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DATE, -1); // Kal ki date
            cal.set(java.util.Calendar.HOUR_OF_DAY, 15);
            cal.set(java.util.Calendar.MINUTE, 30);
            return cal.getTime();
        } else {
            return new Date();
        }
    }

    private boolean isAlreadySyncedToday(SyncTracker tracker) {
        return tracker.getLastRunAt() != null && 
               tracker.getLastRunAt().toLocalDate().equals(LocalDate.now()) && 
               "SUCCESS".equals(tracker.getStatus());
    }

    private Date determineStartDate(SyncTracker tracker , String interval) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        if (tracker.getLastFetchedTimestamp() != null) {
        	Date lastDate = sdf.parse(tracker.getLastFetchedTimestamp());
        	return new Date(lastDate.getTime() + 1000);
        }       
        String start;
        switch (interval) {
            case "5minute" -> start = LocalDate.now().minusDays(60) + " 09:15:00";
            case "15minute" -> start = LocalDate.now().minusDays(200) + " 09:15:00";
            case "60minute" -> start = LocalDate.now().minusDays(400) + " 09:15:00";
            case "day" -> start = LocalDate.now().minusYears(5) + " 09:15:00";   
            case "week" -> start = LocalDate.now().minusYears(5) + " 09:15:00";  
//            case "month" -> {
//                LocalDate startMonth = LocalDate.now().minusYears(2).withDayOfMonth(1); // 5 saal pehle ka 1st
//                LocalDate endMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);   // last month ka start
//                start = startMonth + " 09:15:00";
                // agar API me alag end date pass karna hai:
                // endDate = endMonth + " 15:30:00";  // 15:30 for NSE market close
//            }


            default -> start = "2024-01-01 09:15:00";
    }
        return sdf.parse(start);
        // Agar ekdum naya stock hai, toh aaj subah 9:15 se start
        //String todayStart = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 09:15:00";
        //return sdf.parse(todayStart);
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
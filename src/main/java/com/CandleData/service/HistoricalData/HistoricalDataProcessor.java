package com.CandleData.service.HistoricalData;

import com.CandleData.entity.HistoricalData.HistoricalData;
import com.CandleData.entity.HistoricalData.SyncTracker;
import com.CandleData.entity.stock.Stock;
import com.CandleData.repository.HistoricalData.HistoricalDataRepository;
import com.CandleData.repository.HistoricalData.SyncTrackerRepository;
import static com.CandleData.service.AppConstant.*;

import com.CandleData.service.ErrorCodes;
import com.CandleData.service.Logs.LogService;
import com.CandleData.service.kite.KiteService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricalDataProcessor {
	
	 private static final int DB_BATCH_SIZE = 5000;

    private final KiteService kiteService;
    private final HistoricalDataRepository historicalRepository;
    private final SyncTrackerRepository trackerRepository;
    private final LogService logService;
    
    @Value("${historical.default.start-date}")
    private String defaultStartDate;

    @Transactional
    public void processStockData(Stock stock, String interval, String tableName, SyncTracker tracker) throws Exception, KiteException {
    	if (stock == null || stock.getInstrumentToken() == null || stock.getTradingSymbol() == null) {
    		logService.logError(ErrorCodes.ERR_VALIDATION, "Stock or Token is null", "Table: " + tableName);
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

        if (isAlreadySyncedToday(tracker)) {
            log.info("[SKIP] {} ({}) already synced for today.", symbol, interval);
            return;
        }

        Date fromDate = determineStartDate(tracker, interval);
        Date toDate = determineToDate();
       
        if ("day".equals(interval) || "week".equals(interval) || "month".equals(interval)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            fromDate = sdf.parse(sdf.format(fromDate));
            toDate   = sdf.parse(sdf.format(toDate));
        }

        if (fromDate.after(toDate)) {
           return;
        }

        log.info(">>> [FETCHING] {} ({}) from {} to {}", symbol, interval, fromDate, toDate);

        try {
            List<com.zerodhatech.models.HistoricalData> kiteData = kiteService.getKiteConnect()
                    .getHistoricalData(fromDate, toDate, String.valueOf(stock.getInstrumentToken()), interval, false, true)
                    .dataArrayList;
             if (kiteData == null || kiteData.isEmpty()) {
            	
//            	tracker.setStatus("SUCCESS");
//            	    tracker.setLastRunAt(LocalDateTime.now());
//
//            	    if (tracker.getLastFetchedTimestamp() == null) {
//            	        tracker.setLastFetchedTimestamp(
//            	                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(fromDate)
//            	        );
//            	    }
//
//            	    trackerRepository.save(tracker);

            	 log.warn("[NO DATA] {} ({}) from={} to={}. Tracker not updated.", symbol, interval, fromDate, toDate);  
                 logService.logKiteResponse(symbol, stock.getInstrumentToken(), fromDate, toDate, interval, "Empty response from Kite", "NO_DATA");
             	return;
            	}
             List<HistoricalData> batch = new java.util.ArrayList<>(DB_BATCH_SIZE);
             int totalSaved = 0;

             for (com.zerodhatech.models.HistoricalData d : kiteData) {

                 batch.add(HistoricalData.builder()
                         .id(stock.getInstrumentToken() + "_" + d.timeStamp)
                         .timeStamp(d.timeStamp)
                         .tradingSymbol(symbol)
                         .instrumentToken(stock.getInstrumentToken())
                         .open(d.open)
                         .high(d.high)
                         .low(d.low)
                         .close(d.close)
                         .volume(d.volume)
                         .oi(d.oi)
                         .build());

                 if (batch.size() >= DB_BATCH_SIZE) {
                     historicalRepository.saveBatch(tableName, batch);
                     totalSaved += batch.size();
                     batch.clear();
                 }
             }
             
             // save remaining records
             if (!batch.isEmpty()) {
                 historicalRepository.saveBatch(tableName, batch);
                 totalSaved += batch.size();
             }

                // Update Tracker
                tracker.setLastFetchedTimestamp(kiteData.get(kiteData.size() - 1).timeStamp);
                tracker.setStatus("SUCCESS");
                tracker.setLastRunAt(LocalDateTime.now());
                trackerRepository.save(tracker);
                
                log.info("[SUCCESS] {} ({}) saved {} records", symbol, interval, totalSaved);
             }   
        
        catch (KiteException ke) {
        	String response = ke.getMessage();
            logService.logKiteResponse(symbol,stock.getInstrumentToken(),fromDate,toDate,interval,ke.getMessage(),response);
            logService.logError(ErrorCodes.ERR_KITE_API,ke.getMessage(),"Kite | " + symbol + " | " + interval);
            throw ke;
        }
       catch (Exception e) {
                logService.logError(ErrorCodes.ERR_GENERIC, e.getMessage(),"Processor | " + symbol + " | " + interval);
                throw e;
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

    private Date determineStartDate(SyncTracker tracker, String interval) throws Exception {
        
        DateTimeFormatter kiteFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (tracker.getLastFetchedTimestamp() != null && !tracker.getLastFetchedTimestamp().isEmpty()) {
            try {  
                OffsetDateTime odt = OffsetDateTime.parse(tracker.getLastFetchedTimestamp(), kiteFormatter);
                return Date.from(odt.toInstant().plusSeconds(1));
            } catch (Exception e) {
                Date lastDate = dbFormat.parse(tracker.getLastFetchedTimestamp());
                return new Date(lastDate.getTime() + 1000);
            }
        }
        // Agar ekdum naya stock hai,
        String start;
        interval = interval.toLowerCase();
        switch (interval) {
        	case MINUTE -> start = LocalDate.now().minusDays(70) + MARKET_OPEN_TIME;
            case FIVE_MINUTE -> start = LocalDate.now().minusDays(200) + MARKET_OPEN_TIME;
            case FIFTEEN_MINUTE -> start = LocalDate.now().minusDays(250) + MARKET_OPEN_TIME;
            case SIXTY_MINUTE-> start = LocalDate.now().minusDays(400) + MARKET_OPEN_TIME;
            case DAY -> start = LocalDate.now().minusYears(5) + MARKET_OPEN_TIME;   
            case WEEK -> start = LocalDate.now().minusYears(5) + MARKET_OPEN_TIME;
            default ->   start = defaultStartDate; 
        }
        return dbFormat.parse(start);
        //String todayStart = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 09:15:00";
        //return sdf.parse(todayStart);
    }
    

//    private List<HistoricalData> mapToEntity(List<com.zerodhatech.models.HistoricalData> list, Stock stock) {
//        return list.stream().map(d -> HistoricalData.builder()
//                .id(stock.getInstrumentToken() + "_" + d.timeStamp)
//                .timeStamp(d.timeStamp)
//                .tradingSymbol(stock.getTradingSymbol())
//                .instrumentToken(stock.getInstrumentToken())
//                .open(d.open).high(d.high).low(d.low).close(d.close)
//                .volume(d.volume).oi(d.oi).build()
//        ).collect(Collectors.toList());  
//    }
}
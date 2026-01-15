package com.CandleData.service.HistoricalData;

import static com.CandleData.service.AppConstant.DB_BATCH_SIZE;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import com.CandleData.entity.HistoricalData.HistoricalData;
import com.CandleData.entity.HistoricalData.SyncTracker;
import com.CandleData.entity.stock.Stock;
import com.CandleData.repository.HistoricalData.HistoricalDataRepository;
import com.CandleData.repository.HistoricalData.SyncTrackerRepository;
import com.CandleData.service.ErrorCodes;
import com.CandleData.service.Logs.LogService;
import com.CandleData.service.kite.KiteService;
import com.CandleData.service.util.AppUtils;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricalDataProcessor {
	
    private final KiteService kiteService;
    private final HistoricalDataRepository historicalRepository;
    private final SyncTrackerRepository trackerRepository;
    private final LogService logService;
    private final AppUtils appUtils;
   

    @Transactional
    public void processStockData(Stock stock, String interval, String tableName, SyncTracker tracker) throws Exception, KiteException {
    	if (stock == null || stock.getInstrumentToken() == null || stock.getTradingSymbol() == null) {
    		logService.logError(ErrorCodes.ERR_VALIDATION, "Stock or Token is null", "Table: " + tableName);
            return;
        }
    	
        String symbol = stock.getTradingSymbol();
        
        if(ObjectUtils.isEmpty(tracker) ) {
        	tracker = SyncTracker.builder()
	          .tradingSymbol(symbol)
	          .instrumentToken(stock.getInstrumentToken())
	          .interval(interval)
	          .build();
        }

        if (appUtils.isAlreadySyncedToday(tracker)) {
            log.info("[SKIP] {} ({}) already synced for today.", symbol, interval);
            return;
        }

        Date fromDate = appUtils.determineStartDate(tracker, interval);
        Date toDate = appUtils.determineToDate();
       
        if ("day".equals(interval) || "week".equals(interval)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            fromDate = sdf.parse(sdf.format(fromDate));
            toDate   = sdf.parse(sdf.format(toDate));
        }

        if (fromDate.after(toDate)) {
        log.info("Invalid FromDate and toDate");
           return;
        }

        log.info(">>> [FETCHING] {} ({}) from {} to {}", symbol, interval, fromDate, toDate);

        try {
            List<com.zerodhatech.models.HistoricalData> kiteData = kiteService.getKiteConnect()
                    .getHistoricalData(fromDate, toDate, String.valueOf(stock.getInstrumentToken()), interval, false, true)
                    .dataArrayList;
             if (kiteData == null || kiteData.isEmpty()) {
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
            //throw ke;
        }
       catch (Exception e) {
                logService.logError(ErrorCodes.ERR_GENERIC, e.getMessage(),"Processor | " + symbol + " | " + interval);
                throw e;
            }
        } 
}
package com.CandleData.scheduler;

import static com.CandleData.service.AppConstant.MINUTE;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.CandleData.entity.HistoricalData.SyncTracker;
import com.CandleData.entity.stock.Stock;
import com.CandleData.repository.HistoricalData.HistoricalDataRepository;
import com.CandleData.repository.HistoricalData.SyncTrackerRepository;
import com.CandleData.repository.index.IndexRepository;
import com.CandleData.repository.stock.StockRepository;
import com.CandleData.entity.MarketEntity;
import com.CandleData.service.ErrorCodes;
import com.CandleData.service.HistoricalData.HistoricalDataProcessor;
import com.CandleData.service.HistoricalData.HistoricalSyncEmailService;
import com.CandleData.service.Logs.LogService;
import com.CandleData.service.kite.KiteService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Component
@Slf4j
@RequiredArgsConstructor
public class HistoricalDataScheduler {

    private final StockRepository stockRepository;
    private final HistoricalDataProcessor processor;
    private final HistoricalDataRepository historicalRepository;
    private final IndexRepository indexRepository;
    private final KiteService kiteService;
    private final SyncTrackerRepository trackerRepository;
    private final LogService logService;
    private final HistoricalSyncEmailService historicalSyncEmailService;
    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void runDailySync() throws KiteException {
    	
    	Date startTime = new Date();
        int totalFailures = 0;
        
        if (!isTokenValid()) {           
            log.error("Kite token missing. Login required.");
            logService.logError(ErrorCodes.ERR_KITE_TOKEN, "Access token is null", "Scheduler start");
            return;
        }

        log.info("Historical data sync started at {}", startTime);
        
        // 1. Fetch Stocks and Indices and combine them into one list
        List<MarketEntity> allEntities = new ArrayList<>();
        
        // Fetching Stocks
        allEntities.addAll(stockRepository.findAll().stream()
                .filter(s -> "TCS".equalsIgnoreCase(s.getTradingSymbol()) || 
                		"RELIANCE".equalsIgnoreCase(s.getTradingSymbol()))
                .toList());
        
        // Fetching Indices (Adding all available indices)
        allEntities.addAll(indexRepository.findAll());

        String[] intervals = {MINUTE, "5minute", "15minute", "60minute", "day", "week"};

        for (String interval : intervals) {
            log.info("Processing Interval: [{}]", interval);
           
            List<SyncTracker> trackerList = trackerRepository.findByInterval(interval);         
            Map<Long, SyncTracker> trackerMap = createTrackerMapfromList(trackerList);
            
            for (int i = 0; i < allEntities.size(); i++) {
                MarketEntity entity = allEntities.get(i);
                try {
                    if (i % 50 == 0) log.info("Progress: {}/{} entities done for {}", i, allEntities.size(), interval);
                    
                    // Generic method call for both Stock and Index
                    processor.processMarketData(entity, interval, trackerMap.get(entity.getInstrumentToken()));
                    
                    Thread.sleep(350); 
                    
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logService.logError(ErrorCodes.ERR_THREAD_INTERRUPTED, ie.getMessage(), "Scheduler | " + entity.getTradingSymbol());
                } catch (Exception e) {
                    totalFailures++;
                    logService.logError(ErrorCodes.ERR_GENERIC, e.getMessage(), "Scheduler | symbol=" + entity.getTradingSymbol());

                	try {
                	    SyncTracker failedTracker = trackerMap.get(entity.getInstrumentToken());
                	    if (failedTracker != null) {
                	        failedTracker.setStatus("FAILED");
                	        failedTracker.setLastRunAt(java.time.LocalDateTime.now());
                	        trackerRepository.save(failedTracker);
                	    }
                	} catch (Exception ex) {
                        logService.logError(ErrorCodes.ERR_DB_SAVE, ex.getMessage(), "Updating FAILED tracker");
                    }
                }
            }
        }
        
        Date endTime = new Date();
        log.info("Historical data sync completed at {}", endTime);
     
        // EMAIL notification
        try {
            historicalSyncEmailService.sendHistoricalSyncCompletionMail(
            		"shubangipachauri7@gmail.com",
                    allEntities.stream().map(MarketEntity::getTradingSymbol).toList(),
                    List.of(intervals),
                    startTime,
                    endTime,
                    allEntities.size(),
                    totalFailures
            );
        } catch (Exception e) {
            log.error("Failed to send historical sync completion email", e);
        }
    }
    
    private Map<Long, SyncTracker> createTrackerMapfromList(List<SyncTracker> trackerList) {
    	if (trackerList == null || trackerList.isEmpty()) {
            return new HashMap<>();
        }
    	return trackerList.stream()
                .collect(Collectors.toMap(
                		SyncTracker::getInstrumentToken, 
                    tracker -> tracker,                                      
                    (existing, replacement) -> existing                      
                ));
    }
    
	private boolean isTokenValid() {
        try {
            return kiteService.getKiteConnect().getAccessToken() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
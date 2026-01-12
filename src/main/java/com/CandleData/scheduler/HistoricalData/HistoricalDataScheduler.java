package com.CandleData.scheduler.HistoricalData;

import static com.CandleData.service.AppConstant.MINUTE;

import java.text.SimpleDateFormat;
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
import com.CandleData.repository.stock.StockRepository;
import com.CandleData.service.HistoricalData.HistoricalDataProcessor;
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
    private final KiteService kiteService;
    private final SyncTrackerRepository trackerRepository;
    
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

        String[] intervals = {MINUTE,"5minute", "15minute", "60minute", "day", "week"};
        String monthYear = new SimpleDateFormat("MMM_yyyy").format(new Date()).toUpperCase();

        for (String interval : intervals) {
            log.info("Processing Interval: [{}]", interval);
            String tableName = interval + "_HistoricalData_EQ_" + monthYear;
            historicalRepository.createTableIfNotExist(tableName);
            
            log.info("Interval [{}] started", interval);
            List<SyncTracker> tracker = trackerRepository
                    .findByInterval(interval);
            
            Map<Long, SyncTracker> map = createTrackerMapfromList(tracker);
            
            for (int i = 0; i < stocks.size(); i++) {
                Stock stock = stocks.get(i);
                try {
                    if (i % 50 == 0) log.info("Progress: {}/{} stocks done for {}", i, stocks.size(), interval);
                    
                    processor.processStockData(stock, interval, tableName, map.get(stock.getInstrumentToken()));
                    Thread.sleep(350); 
                } catch (Exception e) {
                	 log.error("Error processing stock [{}] | interval [{}] | token [{}]",
                             stock.getTradingSymbol(),
                             interval,
                             stock.getInstrumentToken(),
                             e
                     );
                	 try {
                	        SyncTracker failedTracker = map.get(stock.getInstrumentToken());
                	        if (failedTracker != null) {
                	            failedTracker.setStatus("FAILED");
                	            failedTracker.setLastRunAt(java.time.LocalDateTime.now());
                	            trackerRepository.save(failedTracker);
                	        }
                	    } catch (Exception ex) {
                	        log.error("Failed to update FAILED status for {}", stock.getTradingSymbol(), ex);
                	    }
                }
            }
        }
        log.info("Historical data sync completed at {}", new Date());
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
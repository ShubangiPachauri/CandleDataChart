package com.CandleData.service.HistoricalData;

import com.CandleData.entity.HistoricalData.HistoricalData;
import com.CandleData.entity.HistoricalData.SyncTracker;
import com.CandleData.entity.stock.Stock;
import com.CandleData.repository.HistoricalData.HistoricalDataRepository;
import com.CandleData.repository.HistoricalData.SyncTrackerRepository;
import com.CandleData.service.kite.KiteService;
import com.CandleData.service.util.AppUtils;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricalDataProcessor {

    private final KiteService kiteService;
    private final HistoricalDataRepository historicalRepository;
    private final SyncTrackerRepository trackerRepository;
    private final AppUtils appUtils;

    @Transactional
    public void processStockData(Stock stock, String interval, SyncTracker tracker) throws Exception, KiteException {
        //Initial Checks
        if (stock == null) return;
        SyncTracker currentTracker = (tracker != null) ? tracker : prepareNewTracker(stock, interval);

        if (appUtils.isAlreadySyncedToday(currentTracker)) {
            log.info("[SKIP] {} ({}) already synced.", stock.getTradingSymbol(), interval);
            return;
        }

        Date fromDate = appUtils.determineStartDate();
        Date toDate = appUtils.determineToDate();
        if (fromDate.after(toDate)) return;

        //Fetch Data
        List<com.zerodhatech.models.HistoricalData> kiteData = fetchKiteData(stock, interval, fromDate, toDate);
        if (kiteData.isEmpty()) return;

        //Map & Save
        List<HistoricalData> entities = kiteData.stream()
                .map(d -> mapToEntity(stock, d))
                .toList();

        historicalRepository.saveBatch(interval, entities);

        // Update Status
        updateTracker(currentTracker, kiteData.get(kiteData.size() - 1).timeStamp);
        log.info("[SUCCESS] {} ({}) - Records: {}", stock.getTradingSymbol(), interval, entities.size());
    }

    private List<com.zerodhatech.models.HistoricalData> fetchKiteData(Stock stock, String interval, Date from, Date to) throws Exception, KiteException {
        return kiteService.getKiteConnect()
                .getHistoricalData(from, to, String.valueOf(stock.getInstrumentToken()), interval, false, true)
                .dataArrayList;
    }

    private HistoricalData mapToEntity(Stock stock, com.zerodhatech.models.HistoricalData d) {
        // 1. Kite format ko MySQL format mein convert karein
        // Kite input: "2026-01-23T09:15:00+0530"
        // MySQL output: "2026-01-23 09:15:00"
        String mysqlTimestamp = d.timeStamp
                .replace("T", " ")
                .split("\\+")[0];

        return HistoricalData.builder()
                .id(stock.getInstrumentToken() + "_" + mysqlTimestamp) // Unique ID
                .timeStamp(mysqlTimestamp) // Sahi date format
                .tradingSymbol(stock.getTradingSymbol())
                .instrumentToken(stock.getInstrumentToken())
                .open(d.open)
                .high(d.high)
                .low(d.low)
                .close(d.close)
                .volume(d.volume)
                .oi(d.oi)
                .build();
    }

    private SyncTracker prepareNewTracker(Stock stock, String interval) {
        return SyncTracker.builder()
                .tradingSymbol(stock.getTradingSymbol())
                .instrumentToken(stock.getInstrumentToken())
                .interval(interval).build();
    }

    private void updateTracker(SyncTracker tracker, String lastTime) {
        tracker.setLastFetchedTimestamp(lastTime);
        tracker.setStatus("SUCCESS");
        tracker.setLastRunAt(java.time.LocalDateTime.now());
        trackerRepository.save(tracker);
    }
}
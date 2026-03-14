package com.CandleData.service.HistoricalData;

import com.CandleData.entity.MarketEntity;
import com.CandleData.entity.HistoricalData.HistoricalData;
import com.CandleData.entity.HistoricalData.SyncTracker;
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
    public void processMarketData(MarketEntity entity, String interval, SyncTracker tracker) throws KiteException {
        try {
            if (entity == null || entity.getInstrumentToken() == null) return;

            SyncTracker currentTracker = (tracker != null) ? tracker : prepareNewTracker(entity, interval);

            if (appUtils.isAlreadySyncedToday(currentTracker)) {
                log.debug("[SKIP] {} ({}) already synced.", entity.getTradingSymbol(), interval);
                return;
            }

            Date fromDate = appUtils.determineStartDate(interval);
            Date toDate = appUtils.determineToDate();

            List<com.zerodhatech.models.HistoricalData> kiteData = fetchKiteData(entity, interval, fromDate, toDate);
            
            if (kiteData == null || kiteData.isEmpty()) {
                log.warn("[NO DATA] No records for {} in {}", entity.getTradingSymbol(), interval);
                return;
            }

            List<HistoricalData> entities = kiteData.stream()
                    .map(d -> mapToEntity(entity, d))
                    .toList();

            historicalRepository.deleteOldData(interval, entity.getTradingSymbol());
            
            historicalRepository.saveBatch(interval, entities);
            updateTrackerSuccess(currentTracker, kiteData.get(kiteData.size() - 1).timeStamp);
            
            log.info("[SUCCESS] {} ({}) - Records: {}", entity.getTradingSymbol(), interval, entities.size());

        } catch (Exception e) {
            log.error("[ERROR] Failed processing {} ({}): {}", entity.getTradingSymbol(), interval, e.getMessage());
            updateTrackerStatus(tracker, "FAILED");
        }
    }

    private List<com.zerodhatech.models.HistoricalData> fetchKiteData(MarketEntity entity, String interval, Date from, Date to) throws Exception, KiteException {
        return kiteService.getKiteConnect()
                .getHistoricalData(from, to, String.valueOf(entity.getInstrumentToken()), interval, false, true)
                .dataArrayList;
    }

    private HistoricalData mapToEntity(MarketEntity entity, com.zerodhatech.models.HistoricalData d) {
        String mysqlTimestamp = d.timeStamp.replace("T", " ").split("\\+")[0];
        return HistoricalData.builder()
                .id(entity.getInstrumentToken() + "_" + mysqlTimestamp)
                .timeStamp(mysqlTimestamp)
                .tradingSymbol(entity.getTradingSymbol())
                .instrumentToken(entity.getInstrumentToken())
                .open(d.open).high(d.high).low(d.low).close(d.close)
                .volume(d.volume).oi(d.oi).build();
    }

    private SyncTracker prepareNewTracker(MarketEntity entity, String interval) {
        return SyncTracker.builder()
                .tradingSymbol(entity.getTradingSymbol())
                .instrumentToken(entity.getInstrumentToken())
                .interval(interval).build();
    }

    private void updateTrackerSuccess(SyncTracker tracker, String lastTime) {
        tracker.setLastFetchedTimestamp(lastTime);
        updateTrackerStatus(tracker, "SUCCESS");
    }

    private void updateTrackerStatus(SyncTracker tracker, String status) {
        if (tracker == null) return;
        tracker.setStatus(status);
        tracker.setLastRunAt(java.time.LocalDateTime.now());
        trackerRepository.save(tracker);
    }
}
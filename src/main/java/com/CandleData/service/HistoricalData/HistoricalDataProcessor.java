package com.CandleData.service.HistoricalData;

import static com.CandleData.service.AppConstant.DB_BATCH_SIZE;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

import org.json.JSONException;
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
import com.CandleData.service.util.HistoricalTableNameUtil;
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

    // ================= MAIN METHOD (SHORT) =================
    @Transactional
    public void processStockData(Stock stock,String interval,String ignoredTableName,SyncTracker tracker)
    		throws Exception, KiteException {

        if (!isValidStock(stock)) return;

        String symbol = stock.getTradingSymbol();
        tracker = prepareTracker(stock, interval, tracker);

        if (appUtils.isAlreadySyncedToday(tracker)) {
            log.info("[SKIP] {} ({}) already synced for today.", symbol, interval);
            return;
        }

        Date fromDate = appUtils.determineStartDate(tracker, interval);
        Date toDate = normalizeDate(interval, appUtils.determineToDate());

        if (fromDate.after(toDate)) {
            log.info("Invalid date range for {}", symbol);
            return;
        }

        List<com.zerodhatech.models.HistoricalData> kiteData =
                fetchKiteData(stock, interval, fromDate, toDate);

        if (kiteData.isEmpty()) return;

        int totalSaved = saveMonthWiseData(stock, interval, kiteData);

        updateTracker(tracker, kiteData);

        log.info("[SUCCESS] {} ({}) saved {} records", symbol, interval, totalSaved);
    }

    // ================= VALIDATION =================
    private boolean isValidStock(Stock stock) {
        if (stock == null ||
            stock.getInstrumentToken() == null ||
            stock.getTradingSymbol() == null) {

            logService.logError(ErrorCodes.ERR_VALIDATION,"Stock or Token is null","HistoricalDataProcessor");
            return false;
        }
        return true;
    }

    // ================= TRACKER =================
    private SyncTracker prepareTracker(Stock stock, String interval, SyncTracker tracker) {

        if (ObjectUtils.isEmpty(tracker)) {
            return SyncTracker.builder()
                    .tradingSymbol(stock.getTradingSymbol())
                    .instrumentToken(stock.getInstrumentToken())
                    .interval(interval)
                    .build();
        }
        return tracker;
    }

    // ================= DATE NORMALIZE =================
    private Date normalizeDate(String interval, Date date) throws Exception {
        if ("day".equals(interval) || "week".equals(interval)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(sdf.format(date));
        }
        return date;
    }

    // ================= KITE FETCH =================
    private List<com.zerodhatech.models.HistoricalData> fetchKiteData(
            Stock stock, String interval, Date from, Date to) throws KiteException, JSONException, IOException {

        try {
            List<com.zerodhatech.models.HistoricalData> data =
                    kiteService.getKiteConnect()
                            .getHistoricalData(from,to,String.valueOf(stock.getInstrumentToken()),interval,false,true)
                            .dataArrayList;

            if (data == null || data.isEmpty()) {
                logService.logKiteResponse(stock.getTradingSymbol(),stock.getInstrumentToken(),from,to,interval,
                		"Empty response from Kite","NO_DATA");        
                return Collections.emptyList();
            }
            return data;

        } catch (KiteException ke) {
            logService.logKiteResponse(stock.getTradingSymbol(),stock.getInstrumentToken(),from,to,interval,
                    ke.getMessage(),
                    ke.getMessage());

            logService.logError(ErrorCodes.ERR_KITE_API,ke.getMessage(),"Kite | " + stock.getTradingSymbol());
            throw ke;
        }
    }

    // ================= DB SAVE (MONTH + YEAR) =================
    private int saveMonthWiseData(Stock stock,String interval,
      List<com.zerodhatech.models.HistoricalData> kiteData) {
      Map<String, List<HistoricalData>> tableWiseData = new HashMap<>();
      
        for (var d : kiteData) {
            String tableName =
                    HistoricalTableNameUtil.resolveTableName(interval, d.timeStamp);

            tableWiseData
                    .computeIfAbsent(tableName, k -> new ArrayList<>())
                    .add(mapToEntity(stock, d));
        }

        int totalSaved = 0;

        for (Map.Entry<String, List<HistoricalData>> entry : tableWiseData.entrySet()) {

            String tableName = entry.getKey();
            List<HistoricalData> records = entry.getValue();

            historicalRepository.createTableIfNotExist(tableName);

            List<HistoricalData> batch = new ArrayList<>(DB_BATCH_SIZE);

            for (HistoricalData data : records) {
                batch.add(data);

                if (batch.size() >= DB_BATCH_SIZE) {
                    historicalRepository.saveBatch(tableName, batch);
                    totalSaved += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                historicalRepository.saveBatch(tableName, batch);
                totalSaved += batch.size();
            }
        }

        return totalSaved;
    }

    // ================= ENTITY MAPPER =================
    private HistoricalData mapToEntity(
            Stock stock,
            com.zerodhatech.models.HistoricalData d) {

        return HistoricalData.builder()
                .id(stock.getInstrumentToken() + "_" + d.timeStamp)
                .timeStamp(d.timeStamp)
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

    // ================= TRACKER UPDATE =================
    private void updateTracker(
            SyncTracker tracker,
            List<com.zerodhatech.models.HistoricalData> kiteData) {

        tracker.setLastFetchedTimestamp(
                kiteData.get(kiteData.size() - 1).timeStamp);
        tracker.setStatus("SUCCESS");
        tracker.setLastRunAt(LocalDateTime.now());

        trackerRepository.save(tracker);
    }
}

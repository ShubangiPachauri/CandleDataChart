package com.CandleData.service.HistoricalData;

import com.CandleData.entity.HistoricalData.OtherHistoricalData;
import com.CandleData.entity.stock.Stock;
import com.CandleData.repository.HistoricalData.OtherHistoricalDataRepository;
import com.CandleData.repository.stock.StockRepository;
import com.CandleData.service.kite.KiteService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtherHistoricalDataService {

    private final KiteService kiteService;
    private final StockRepository stockRepository;
    private final OtherHistoricalDataRepository repository;

    private String resolveTableName(String month, String year) {
        return "OtherHistoricalData_EQ_" + month + "_" + year;
    }

    @Async
    public void syncData(String month, String year, String timeframe) throws KiteException {
        try {
            String tableName = resolveTableName(month, year);
            repository.createTableIfNotExist(tableName);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date fromDate = sdf.parse(year + "-" + month + "-01 09:15:00");
            Date toDate = new Date();

            List<Stock> stocks = stockRepository.findAll();
            log.info("STARTING GLOBAL SYNC | Table: {} | Timeframe: {} | Total Stocks: {}", 
                     tableName, timeframe, stocks.size());

            int count = 0;
            for (Stock stock : stocks) {
                count++;
                try {
                    log.info("[{}/{}] Syncing Stock: {} (Token: {})", 
                             count, stocks.size(), stock.getTradingSymbol(), stock.getInstrumentToken());
                    
                    fetchAndSave(stock, fromDate, toDate, tableName, timeframe);
                    
                    // Kite Rate Limit: 10 calls per second. 200ms sleep is safe.
                    Thread.sleep(200); 
                } catch (Exception e) {
                    log.error("XXXX FAILED Stock: {} | Error: {}", stock.getTradingSymbol(), e.getMessage());
                }
            }
            log.info(">>>>>> COMPLETED GLOBAL SYNC for table {}", tableName);
        } catch (Exception e) {
            log.error("CRITICAL Global Sync Error: {}", e.getMessage());
        }
    }

    private void fetchAndSave(Stock stock, Date fromDate, Date toDate, String tableName, String timeframe) throws Exception, KiteException {
        KiteConnect kite = kiteService.getKiteConnect();
        
        List<com.zerodhatech.models.HistoricalData> response = 
            kite.getHistoricalData(fromDate, toDate, String.valueOf(stock.getInstrumentToken()), timeframe, false, true).dataArrayList;

        if (response != null && !response.isEmpty()) {
            List<OtherHistoricalData> entities = response.stream().map(kd -> 
                OtherHistoricalData.builder()
                    .id(stock.getInstrumentToken() + "_" + kd.timeStamp + "_" + timeframe)
                    .timeStamp(kd.timeStamp)
                    .tradingSymbol(stock.getTradingSymbol())
                    .instrumentToken(stock.getInstrumentToken())
                    .open(kd.open)
                    .high(kd.high)
                    .low(kd.low)
                    .close(kd.close)
                    .volume(kd.volume)
                    .oi(kd.oi)
                    .timeframe(timeframe)
                    .build()
            ).collect(Collectors.toList());

            repository.saveBatch(tableName, entities);
            log.info("SUCCESS: Saved {} records for {}", entities.size(), stock.getTradingSymbol());
        } else {
            log.warn("EMPTY: No records found for {} from API", stock.getTradingSymbol());
        }
    }

    public List<OtherHistoricalData> getHistoricalDataFromDB(String instrumentToken, String fromDate, String toDate, String timeframe) {
        try {
            String year = fromDate.substring(0, 4);
            String month = fromDate.substring(5, 7);
            String tableName = resolveTableName(month, year);

            log.info("FETCH: Requesting data for Token: {} from {}", instrumentToken, tableName);

            List<Object[]> results = repository.fetchData(tableName, instrumentToken, fromDate, toDate);

            log.info("FETCH SUCCESS: Found {} records", results.size());

            return results.stream().map(row -> OtherHistoricalData.builder()
                    .id((String) row[0])
                    .timeStamp((String) row[1])
                    .tradingSymbol((String) row[2])
                    .instrumentToken(((Number) row[3]).longValue())
                    .open((Double) row[4])
                    .high((Double) row[5])
                    .low((Double) row[6])
                    .close((Double) row[7])
                    .volume(((Number) row[8]).longValue())
                    .oi(((Number) row[9]).longValue())
                    .timeframe((String) row[10])
                    .build()
            ).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("DB Fetch Error: {}", e.getMessage());
            return List.of();
        }
    }
}
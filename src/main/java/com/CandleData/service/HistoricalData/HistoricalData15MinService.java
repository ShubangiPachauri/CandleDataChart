package com.CandleData.service.HistoricalData;

import com.CandleData.entity.HistoricalData.HistoricalData;
import com.CandleData.entity.stock.Stock;
import com.CandleData.repository.HistoricalData.HistoricalDataRepository;
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
public class HistoricalData15MinService {

    private final KiteService kiteService;
    private final StockRepository stockRepository;
    private final HistoricalDataRepository historicalRepository;

    @Async // Background mein chalne ke liye
    public void syncMonthlyData15Min(String month, String year) throws KiteException {
        try {
            String tableName = "15MinuteHistoricalData_EQ_" + month + "_" + year;
            historicalRepository.createTableIfNotExist(tableName);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date fromDate = sdf.parse(year + "-" + month + "-01 09:15:00");
            Date toDate = new Date();

            List<Stock> stocks = stockRepository.findAll();
            log.info("Total stocks found: {}. Starting background sync...", stocks.size());

            for (Stock stock : stocks) {
                try {
                    fetchAndSave(stock, fromDate, toDate, tableName);
                   
                    Thread.sleep(400); 
                } catch (Exception e) {
                    log.error("Error for {}: {}", stock.getTradingSymbol(), e.getMessage());
                }
            }
            log.info("Background sync completed for {}/{}", month, year);
        } catch (Exception e) {
            log.error("Global Error in Async Sync: {}", e.getMessage());
        }
    }
    
    private void fetchAndSave(Stock stock, Date fromDate, Date toDate, String tableName) throws Exception, KiteException {
        KiteConnect kite = kiteService.getKiteConnect();
        
        // Interval set to "15minute"
        List<com.zerodhatech.models.HistoricalData> dataArrayList = 
            kite.getHistoricalData(fromDate, toDate, String.valueOf(stock.getInstrumentToken()), "15minute", false, true).dataArrayList;

        if (dataArrayList != null && !dataArrayList.isEmpty()) {
            List<HistoricalData> entities = dataArrayList.stream().map(kiteData -> 
                HistoricalData.builder()
                    .id(stock.getInstrumentToken() + "_" + kiteData.timeStamp)
                    .timeStamp(kiteData.timeStamp)
                    .tradingSymbol(stock.getTradingSymbol())
                    .instrumentToken(stock.getInstrumentToken())
                    .open(kiteData.open)
                    .high(kiteData.high)
                    .low(kiteData.low)
                    .close(kiteData.close)
                    .volume(kiteData.volume)
                    .oi(kiteData.oi)
                    .build()
            ).collect(Collectors.toList());

            historicalRepository.saveBatch(tableName, entities);
            log.info("Saved {} (15Min) records for {}", entities.size(), stock.getTradingSymbol());
        }
    }
}
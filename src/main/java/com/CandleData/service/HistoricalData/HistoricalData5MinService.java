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
public class HistoricalData5MinService {

    private final KiteService kiteService;
    private final StockRepository stockRepository;
    private final HistoricalDataRepository historicalRepository;

    @Async 
    public void syncMonthlyData(String month, String year) throws KiteException {
        try {
            String tableName = "5MinuteHistoricalData_EQ_" + month + "_" + year;
            historicalRepository.createTableIfNotExist(tableName);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date fromDate = sdf.parse(year + "-" + month + "-01 09:15:00");
            Date toDate = new Date(); 

            List<Stock> stocks = stockRepository.findAll();
            log.info("Starting 5Min Sync for {} stocks in background...", stocks.size());

            for (Stock stock : stocks) {
                try {
                    fetchAndSaveForSingleStock(stock, fromDate, toDate, tableName);
                
                    Thread.sleep(400); 
                } catch (Exception e) {
                    log.error("Error fetching 5min data for {}: {}", stock.getTradingSymbol(), e.getMessage());
                }
            }
            log.info("COMPLETED: 5Min Sync for {}/{}", month, year);
        } catch (Exception e) {
            log.error("Fatal Error in 5Min Sync: {}", e.getMessage());
        }
    }

    private void fetchAndSaveForSingleStock(Stock stock, Date fromDate, Date toDate, String tableName) throws Exception, KiteException {
        KiteConnect kite = kiteService.getKiteConnect();
        
        List<com.zerodhatech.models.HistoricalData> dataArrayList = 
            kite.getHistoricalData(fromDate, toDate, String.valueOf(stock.getInstrumentToken()), "5minute", false, true).dataArrayList;

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
            log.info("Saved {} (5min) records for {}", entities.size(), stock.getTradingSymbol());
        }
    }
}
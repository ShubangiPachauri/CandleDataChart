package com.CandleData.service.stock;

import com.CandleData.entity.stock.Stock;
import com.CandleData.repository.stock.StockRepository;
import com.CandleData.service.kite.KiteService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Instrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockService {

    private final KiteService kiteService;
    private final StockRepository stockRepository;

    @Transactional(rollbackFor = Exception.class)
    public void syncAllInstruments() throws IOException, KiteException {
        log.info("Stock Sync Started: Fetching instruments from Kite Connect...");
        KiteConnect kiteConnect = kiteService.getKiteConnect();
        
        List<Instrument> allInstruments = kiteConnect.getInstruments();
        
        if (allInstruments == null || allInstruments.isEmpty()) {
            log.warn("No instruments received from Kite API.");
            return;
        }

        log.info("Total instruments received: {}. Filtering for NSE/BSE Equity...", allInstruments.size());

        Map<String, Stock> filteredStocksMap = allInstruments.stream()
                .filter(inst -> "NSE".equals(inst.getExchange()) || "BSE".equals(inst.getExchange()))
                .filter(inst -> "EQ".equals(inst.getInstrument_type()))
                .collect(Collectors.toMap(
                        Instrument::getTradingsymbol,
                        inst -> Stock.builder()
                                .tradingSymbol(inst.getTradingsymbol())
                                .exchange(inst.getExchange())
                                .instrumentToken(inst.getInstrument_token())
                                .build(),
                        (existing, replacement) -> {                           
                            return "NSE".equals(replacement.getExchange()) ? replacement : existing;
                        }
                ));

        log.info("Filtering complete. Unique stocks identified: {}", filteredStocksMap.size());

        try {
            log.info("Cleaning old stock data and saving new records...");
            stockRepository.deleteAllInBatch(); 
            stockRepository.saveAll(filteredStocksMap.values());
            log.info("Stock sync completed successfully. Total records saved: {}", filteredStocksMap.size());
        } catch (Exception e) {
            log.error("Database error during stock sync: {}", e.getMessage());
            throw e;
        }
    }
}
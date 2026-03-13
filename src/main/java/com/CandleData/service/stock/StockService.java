package com.CandleData.service.stock;

import com.CandleData.entity.stock.Stock;
import com.CandleData.repository.stock.StockRepository;
import com.CandleData.service.kite.KiteService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.LTPQuote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final KiteService kiteService;
    private final StockRepository stockRepository;
    private final Nifty500DownloaderService nifty500DownloaderService;

    public void syncNifty500Stocks() throws Exception, KiteException {

        log.info("Starting Nifty500 stock sync");

        // 1️⃣ Download Nifty500 symbols
        Set<String> nifty500Symbols =
                nifty500DownloaderService.downloadNifty500Symbols();

        if (nifty500Symbols.isEmpty()) {
            throw new RuntimeException("Nifty500 symbols not downloaded");
        }

        // 2️⃣ Kite instruments
        KiteConnect kiteConnect = kiteService.getKiteConnect();

        List<Instrument> instruments = kiteConnect.getInstruments("NSE");

        // 3️⃣ Filter Nifty500 stocks
        List<Instrument> filtered = instruments.stream()

                .filter(i -> "EQ".equals(i.getInstrument_type()))

                .filter(i -> nifty500Symbols.contains(i.getTradingsymbol()))

                .collect(Collectors.toList());

        log.info("Filtered Nifty500 stocks: {}", filtered.size());

        if (filtered.isEmpty()) {
            throw new RuntimeException("No Nifty500 stocks found from instruments");
        }

        // 4️⃣ Prepare LTP symbols
        List<String> ltpSymbols = filtered.stream()
                .map(i -> "NSE:" + i.getTradingsymbol())
                .collect(Collectors.toList());

        Map<String, LTPQuote> ltpData = kiteConnect.getLTP(ltpSymbols.toArray(new String[0]));

        List<Stock> stocksToSave = new ArrayList<>();

        for (Instrument inst : filtered) {

            String symbol = inst.getTradingsymbol();

            String ltpKey = "NSE:" + symbol;

            double lastPrice = 0.0;

            if (ltpData.containsKey(ltpKey)) {
                lastPrice = ltpData.get(ltpKey).lastPrice;
            }

            Optional<Stock> existingStock = stockRepository.findByTradingSymbol(symbol);

            Stock stock;

            if (existingStock.isPresent()) {

                stock = existingStock.get();

                stock.setExchange(inst.getExchange());
                stock.setInstrumentToken(inst.getInstrument_token());
                stock.setLastPrice(lastPrice);

            } else {

                stock = Stock.builder()
                        .tradingSymbol(symbol)
                        .exchange(inst.getExchange())
                        .instrumentToken(inst.getInstrument_token())
                        .lastPrice(lastPrice)
                        .build();
            }

            stocksToSave.add(stock);
        }

        stockRepository.saveAll(stocksToSave);

        log.info("Nifty500 stocks synced successfully. Total saved: {}", stocksToSave.size());
    }
}
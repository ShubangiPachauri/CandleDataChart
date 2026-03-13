package com.CandleData.service.index;

import com.CandleData.entity.index.IndexData;
import com.CandleData.repository.index.IndexRepository;
import com.CandleData.service.kite.KiteService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.LTPQuote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;
@Service
@RequiredArgsConstructor
@Slf4j
public class IndexService {

    private final KiteService kiteService;
    private final IndexRepository indexRepository;

    private static final List<String> INDEX_SYMBOLS = List.of(
            "NSE:NIFTY 50",
            "NSE:NIFTY BANK",
            "NSE:NIFTY NEXT 50",
            "NSE:NIFTY MIDCAP 100",
            "NSE:NIFTY 500"
    );

    public void syncTopIndices() throws Exception, KiteException {

        log.info("Starting Top Indices Sync");

        KiteConnect kiteConnect = kiteService.getKiteConnect();

        Map<String, LTPQuote> ltpData =
                kiteConnect.getLTP(INDEX_SYMBOLS.toArray(new String[0]));

        List<IndexData> saveList = new ArrayList<>();

        for (String key : INDEX_SYMBOLS) {

            String symbol = key.replace("NSE:", "");

            double price = 0.0;

            if (ltpData.containsKey(key)) {
                price = ltpData.get(key).lastPrice;
            }

            Optional<IndexData> existing =
                    indexRepository.findByTradingSymbol(symbol);

            IndexData index;

            if (existing.isPresent()) {

                index = existing.get();
                index.setLastPrice(price);
                index.setStatus(1);

            } else {

                index = IndexData.builder()
                        .indexName(symbol)
                        .tradingSymbol(symbol)
                        .exchange("NSE")
                        .lastPrice(price)
                        .status(1)
                        .build();
            }

            saveList.add(index);
        }

        indexRepository.saveAll(saveList);

        log.info("Indices synced successfully: {}", saveList.size());
    }
}
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

        // 1. Saare NSE instruments fetch karo (Kyuki symbols NSE ke hain)
        List<Instrument> allInstruments = kiteConnect.getInstruments("NSE");
        
        // 2. LTP fetch karo (sirf price ke liye)
        Map<String, LTPQuote> ltpData = kiteConnect.getLTP(INDEX_SYMBOLS.toArray(new String[0]));

        List<IndexData> saveList = new ArrayList<>();

        for (String fullSymbol : INDEX_SYMBOLS) {
            String cleanSymbol = fullSymbol.replace("NSE:", "");
            
            // 3. Master list mein se matching instrument dhoondo token nikaalne ke liye
            Instrument match = allInstruments.stream()
                    .filter(inst -> inst.tradingsymbol.equals(cleanSymbol))
                    .findFirst()
                    .orElse(null);

            Optional<IndexData> existing = indexRepository.findByTradingSymbol(cleanSymbol);
            IndexData index = existing.orElse(new IndexData());

            index.setTradingSymbol(cleanSymbol);
            index.setIndexName(cleanSymbol);
            index.setExchange("NSE");
            index.setStatus(1);

            // Price update
            if (ltpData.containsKey(fullSymbol)) {
                index.setLastPrice(ltpData.get(fullSymbol).lastPrice);
            }

            // Token update (Yahan se token milega)
            if (match != null) {
                index.setInstrumentToken(match.getInstrument_token());
            }

            saveList.add(index);
        }

        indexRepository.saveAll(saveList);
        log.info("Indices synced with tokens: {}", saveList.size());
    }
 }
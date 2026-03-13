package com.CandleData.service.stock;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
@Service
@Slf4j
public class Nifty500DownloaderService {

    private static final String CSV_URL = "https://archives.nseindia.com/content/indices/ind_nifty500list.csv";

    public Set<String> downloadNifty500Symbols() {
        Set<String> symbols = new HashSet<>();
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(CSV_URL).openConnection();
            
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(new InputStreamReader(connection.getInputStream()));

            for (CSVRecord record : records) {
                String symbol = record.get("Symbol").trim().toUpperCase();
                symbols.add(symbol);
            }
            log.info("Total Nifty500 symbols fetched: {}", symbols.size());
        } catch (Exception e) {
            log.error("Error downloading Nifty500 CSV: {}", e.getMessage());
        }
        return symbols;
    }
}
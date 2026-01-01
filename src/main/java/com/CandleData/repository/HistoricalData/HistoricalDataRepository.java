package com.CandleData.repository.HistoricalData;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.CandleData.entity.HistoricalData.HistoricalData;

import java.util.List;

@Repository
public class HistoricalDataRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void createTableIfNotExist(String tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id VARCHAR(255) PRIMARY KEY, " +
                "time_stamp VARCHAR(255), " +
                "trading_symbol VARCHAR(255), " +
                "instrument_token BIGINT, " +
                "open DOUBLE, " +
                "high DOUBLE, " +
                "low DOUBLE, " +
                "close DOUBLE, " +
                "volume BIGINT, " +
                "oi BIGINT)";
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    @Transactional
    public void saveBatch(String tableName, List<HistoricalData> dataList) {
        if (dataList.isEmpty()) return;

        // INSERT IGNORE use kar rahe hain taaki duplicate key error na aaye
        String sql = "INSERT IGNORE INTO " + tableName + " (id, time_stamp, trading_symbol, instrument_token, open, high, low, close, volume, oi) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        int batchSize = 500; // Ek baar mein 500 records insert honge
        for (int i = 0; i < dataList.size(); i++) {
            HistoricalData data = dataList.get(i);
            entityManager.createNativeQuery(sql)
                    .setParameter(1, data.getId())
                    .setParameter(2, data.getTimeStamp())
                    .setParameter(3, data.getTradingSymbol())
                    .setParameter(4, data.getInstrumentToken())
                    .setParameter(5, data.getOpen())
                    .setParameter(6, data.getHigh())
                    .setParameter(7, data.getLow())
                    .setParameter(8, data.getClose())
                    .setParameter(9, data.getVolume())
                    .setParameter(10, data.getOi())
                    .executeUpdate();

            if (i > 0 && i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }
 }
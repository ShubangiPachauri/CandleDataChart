package com.CandleData.repository.HistoricalData;

import com.CandleData.entity.HistoricalData.OtherHistoricalData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class OtherHistoricalDataRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private void validateTableName(String tableName) {
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid Table Name: SQL Injection Risk");
        }
    }

    @Transactional
    public void createTableIfNotExist(String tableName) {
        validateTableName(tableName);
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
                "oi BIGINT, " +
                "timeframe VARCHAR(50))";
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    @Transactional
    public void saveBatch(String tableName, List<OtherHistoricalData> dataList) {
        if (dataList.isEmpty()) return;
        validateTableName(tableName);

        String sql = "INSERT IGNORE INTO " + tableName + " (id, time_stamp, trading_symbol, instrument_token, open, high, low, close, volume, oi, timeframe) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        int batchSize = 500;
        for (int i = 0; i < dataList.size(); i++) {
            OtherHistoricalData data = dataList.get(i);
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
                    .setParameter(11, data.getTimeframe())
                    .executeUpdate();

            if (i > 0 && i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> fetchData(String tableName, String instrumentToken, String fromDate, String toDate) {
        validateTableName(tableName);
        String sql = "SELECT id, time_stamp, trading_symbol, instrument_token, open, high, low, close, volume, oi, timeframe " +
                     "FROM " + tableName + " " +
                     "WHERE instrument_token = ? AND time_stamp BETWEEN ? AND ? " +
                     "ORDER BY time_stamp ASC";
        
        return entityManager.createNativeQuery(sql)
                .setParameter(1, instrumentToken)
                .setParameter(2, fromDate)
                .setParameter(3, toDate)
                .getResultList();
    }
}
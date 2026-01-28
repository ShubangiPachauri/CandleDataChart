package com.CandleData.repository.HistoricalData;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PartitionRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public boolean partitionExists(String table, String partition) {
        String sql = """
            SELECT COUNT(*) FROM information_schema.PARTITIONS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = :table
              AND PARTITION_NAME = :partition
        """;

        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("table", table)
                .setParameter("partition", partition)
                .getSingleResult();

        return count.intValue() > 0;
    }


    public void createPartition(String table, String partition, String lessThanExpr) {
        String sql = String.format("""
            ALTER TABLE %s
            REORGANIZE PARTITION p_future INTO (
                PARTITION %s VALUES LESS THAN (%s),
                PARTITION p_future VALUES LESS THAN MAXVALUE
            )
        """, table, partition, lessThanExpr);

        entityManager.createNativeQuery(sql).executeUpdate();
    }
}

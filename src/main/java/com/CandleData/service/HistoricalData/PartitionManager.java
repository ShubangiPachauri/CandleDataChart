package com.CandleData.service.HistoricalData;

import com.CandleData.repository.HistoricalData.PartitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartitionManager {

    private final PartitionRepository repository;
    private final PartitionPlanner planner;

    private static final List<String> TABLES = List.of(
            "minute_historicaldata_eq",
            "5minute_historicaldata_eq",
            "15minute_historicaldata_eq",
            "60minute_historicaldata_eq",
            "day_historicaldata_eq",
            "week_historicaldata_eq"
    );

    public void createNextMonthPartitions() {

        String partition = planner.nextPartitionName();
        String lessThan = planner.nextPartitionLessThan();

        log.info("Creating future partition [{}] LESS THAN {}", partition, lessThan);

        for (String table : TABLES) {
            handleTable(table, partition, lessThan);
        }

        log.info("Partition process completed for [{}]", partition);
    }


    private void handleTable(String table, String partition, String lessThan) {
        try {
            if (repository.partitionExists(table, partition)) {
                log.info("[SKIP] {} already has partition {}", table, partition);
                return;
            }

            repository.createPartition(table, partition, lessThan);
            log.info("[SUCCESS] Partition {} created on {}", partition, table);

        } catch (Exception e) {
            log.error("[FAILED] Partition {} on {}", partition, table, e);
        }
    }
}

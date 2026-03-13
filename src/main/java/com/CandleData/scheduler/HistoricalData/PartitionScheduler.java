//package com.CandleData.scheduler.HistoricalData;
//
//import com.CandleData.service.HistoricalData.PartitionManager;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class PartitionScheduler {
//
//    private final PartitionManager partitionManager;
//
//    // 1st of every month at 9 AM
//    @Scheduled(cron = "0 0 9 1 * ?")
//    public void runMonthlyPartitionJob() {
//
//        log.info("Monthly partition scheduler started");
//
//        try {
//            partitionManager.createNextMonthPartitions();
//            log.info("Monthly partition scheduler completed");
//
//        } catch (Exception e) {
//            log.error("Monthly partition scheduler failed", e);
//        }
//    }
//}

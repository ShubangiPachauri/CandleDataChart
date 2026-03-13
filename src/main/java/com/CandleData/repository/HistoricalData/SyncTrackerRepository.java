//package com.CandleData.repository.HistoricalData;
//
//import com.CandleData.entity.HistoricalData.SyncTracker;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface SyncTrackerRepository extends JpaRepository<SyncTracker, Long> {
//    Optional<SyncTracker> findByInstrumentTokenAndInterval(Long token, String interval);
//
//	List<SyncTracker> findByInterval(String interval);
//}
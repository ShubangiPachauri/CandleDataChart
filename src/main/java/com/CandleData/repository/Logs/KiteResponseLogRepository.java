package com.CandleData.repository.Logs;

import com.CandleData.entity.Logs.KiteResponseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KiteResponseLogRepository extends JpaRepository<KiteResponseLog, Long> {
	
}

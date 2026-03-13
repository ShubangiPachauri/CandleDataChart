package com.CandleData.repository.index;

import com.CandleData.entity.index.IndexData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IndexRepository extends JpaRepository<IndexData, Long> {

    Optional<IndexData> findByTradingSymbol(String tradingSymbol);

}
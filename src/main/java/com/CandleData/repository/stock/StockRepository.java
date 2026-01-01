package com.CandleData.repository.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CandleData.entity.stock.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
   
}
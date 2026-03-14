package com.CandleData.entity.stock;
import com.CandleData.entity.MarketEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stocks500")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock implements MarketEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "exchange")
    private String exchange;
    
    @Column(name = "trading_symbol", unique = true, nullable = false)
    private String tradingSymbol;

    @Column(name = "instrument_token")
    private Long instrumentToken;
    
    @Column(name = "last_price")
    private Double lastPrice; 
    
    @Column(name = "status")
    private Integer status; // 1 = active, 0 = inactive
}
package com.CandleData.entity.stock;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "exchange")
    private String exchange;
    
    @Column(name = "trading_symbol", unique = true, nullable = false)
    private String tradingSymbol;

    @Column(name = "instrument_token")
    private Long instrumentToken;
}
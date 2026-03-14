package com.CandleData.entity.index;

import com.CandleData.entity.MarketEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "indices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexData implements MarketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String indexName;

    private String tradingSymbol;
    
    private Long instrumentToken;

    private Double lastPrice;

    private String exchange;

    private Integer status;
}
package com.CandleData.entity.index;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "indices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String indexName;

    private String tradingSymbol;

    private Double lastPrice;

    private String exchange;

    private Integer status;
}
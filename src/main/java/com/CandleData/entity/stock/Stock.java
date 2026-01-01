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

    private String exchange;

    @Column(unique = true, nullable = false)
    private String tradingSymbol;

    private Long instrumentToken;
}
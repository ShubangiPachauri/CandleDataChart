package com.CandleData.entity.Logs;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Date;
@Entity
@Table(name = "kite_response_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KiteResponseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tradingSymbol;

    private Long instrumentToken;

    private String intervalType;

    private Date fromDate;

    private Date toDate;

    private String failureMessage;

    @Lob
    @Column(name = "response_received")
    private String responseReceived;

    private LocalDateTime createdAt;
}

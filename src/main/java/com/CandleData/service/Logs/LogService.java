package com.CandleData.service.Logs;

import com.CandleData.entity.Logs.ErrorLog;
import com.CandleData.entity.Logs.KiteResponseLog;
import com.CandleData.repository.Logs.ErrorLogRepository;
import com.CandleData.repository.Logs.KiteResponseLogRepository;
import com.CandleData.service.ErrorCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class LogService {
    private final KiteResponseLogRepository kiteLogRepo;
    private final ErrorLogRepository errorLogRepo;

    public void logKiteResponse(String symbol, Long token, Date from, Date to, String interval, String error, String resp) {
        KiteResponseLog logEntry = KiteResponseLog.builder()
                .tradingSymbol(symbol)
                .instrumentToken(token)
                .fromDate(from)
                .toDate(to)
                .intervalType(interval)
                .failureMessage(error)
                .responseReceived(resp)
                .createdAt(LocalDateTime.now())
                .build();
        kiteLogRepo.save(logEntry);
    }

    public void logError(ErrorCodes code, String actualMsg, String context) {
        ErrorLog errorEntry = ErrorLog.builder()
                .errorCode(code.getCode())
                .errorMessage(code.getMessage())
                .actualMessage(actualMsg)
                .response(context)
                .createdAt(LocalDateTime.now())
                .build();
        errorLogRepo.save(errorEntry);
    }
}
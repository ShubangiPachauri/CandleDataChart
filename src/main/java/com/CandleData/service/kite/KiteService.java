package com.CandleData.service.kite;

import com.CandleData.config.kite.KiteConfig;
import com.CandleData.entity.kite.KiteSession;
import com.CandleData.repository.kite.KiteSessionRepository;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class KiteService {

    private final KiteConfig kiteConfig;
    private final KiteSessionRepository sessionRepository;
    private KiteConnect kiteConnect;

    public String getLoginUrl() {
        return new KiteConnect(kiteConfig.getApiKey()).getLoginURL();
    }

    public void generateSession(String requestToken) throws IOException, KiteException {
        KiteConnect client = new KiteConnect(kiteConfig.getApiKey());
        client.setUserId(kiteConfig.getUserId());

        User user = client.generateSession(requestToken, kiteConfig.getApiSecret());
       
        KiteSession session = KiteSession.builder()
                .userId(kiteConfig.getUserId())
                .accessToken(user.accessToken)
                .publicToken(user.publicToken)
                .loginTime(LocalDateTime.now())
                .build();
        
        sessionRepository.save(session);
        
        client.setAccessToken(user.accessToken);
        this.kiteConnect = client;
        log.info("Session generated and saved for user: {}", user.userId);
    }

    public KiteConnect getKiteConnect() {
        if (this.kiteConnect != null) {
            return this.kiteConnect;
        }

        return sessionRepository.findFirstByOrderByLoginTimeDesc()
                .map(session -> {
                    KiteConnect client = new KiteConnect(kiteConfig.getApiKey());
                    client.setAccessToken(session.getAccessToken());
                    client.setUserId(session.getUserId());
                    this.kiteConnect = client;
                    return client;
                })
                .orElseThrow(() -> new RuntimeException("No active session found. Please login via /api/kite/login"));
    }
}
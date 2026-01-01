package com.CandleData.repository.kite;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CandleData.entity.kite.KiteSession;

import java.util.Optional;

public interface KiteSessionRepository extends JpaRepository<KiteSession, Long> {
    
    Optional<KiteSession> findFirstByOrderByLoginTimeDesc();
}
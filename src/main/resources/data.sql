-- ======================================================
-- 1. MINUTE TABLE
-- ======================================================
CREATE TABLE IF NOT EXISTS minute_historicaldata_eq (
    id VARCHAR(100) NOT NULL,
    instrument_token BIGINT NOT NULL,
    time_stamp DATETIME NOT NULL,
    trading_symbol VARCHAR(50),
    open_price DOUBLE,
    high_price DOUBLE,
    low_price DOUBLE,
    close_price DOUBLE,
    volume BIGINT,
    oi BIGINT DEFAULT 0,
    PRIMARY KEY (id, time_stamp),
    INDEX idx_1m_token_time (instrument_token, time_stamp)
) ENGINE=InnoDB
PARTITION BY RANGE (YEAR(time_stamp)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ======================================================
-- 2. 5-MINUTE TABLE
-- ======================================================
CREATE TABLE IF NOT EXISTS 5minute_historicaldata_eq (
    id VARCHAR(100) NOT NULL,
    instrument_token BIGINT NOT NULL,
    time_stamp DATETIME NOT NULL,
    trading_symbol VARCHAR(50),
    open_price DOUBLE,
    high_price DOUBLE,
    low_price DOUBLE,
    close_price DOUBLE,
    volume BIGINT,
    oi BIGINT DEFAULT 0,
    PRIMARY KEY (id, time_stamp),
    INDEX idx_5m_token_time (instrument_token, time_stamp)
) ENGINE=InnoDB
PARTITION BY RANGE (YEAR(time_stamp)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ======================================================
-- 3. 15-MINUTE TABLE
-- ======================================================
CREATE TABLE IF NOT EXISTS 15minute_historicaldata_eq (
    id VARCHAR(100) NOT NULL,
    instrument_token BIGINT NOT NULL,
    time_stamp DATETIME NOT NULL,
    trading_symbol VARCHAR(50),
    open_price DOUBLE,
    high_price DOUBLE,
    low_price DOUBLE,
    close_price DOUBLE,
    volume BIGINT,
    oi BIGINT DEFAULT 0,
    PRIMARY KEY (id, time_stamp),
    INDEX idx_15m_token_time (instrument_token, time_stamp)
) ENGINE=InnoDB
PARTITION BY RANGE (YEAR(time_stamp)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ======================================================
-- 4. 60-MINUTE TABLE
-- ======================================================
CREATE TABLE IF NOT EXISTS 60minute_historicaldata_eq (
    id VARCHAR(100) NOT NULL,
    instrument_token BIGINT NOT NULL,
    time_stamp DATETIME NOT NULL,
    trading_symbol VARCHAR(50),
    open_price DOUBLE,
    high_price DOUBLE,
    low_price DOUBLE,
    close_price DOUBLE,
    volume BIGINT,
    oi BIGINT DEFAULT 0,
    PRIMARY KEY (id, time_stamp),
    INDEX idx_60m_token_time (instrument_token, time_stamp)
) ENGINE=InnoDB
PARTITION BY RANGE (YEAR(time_stamp)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ======================================================
-- 5. DAY TABLE
-- ======================================================
CREATE TABLE IF NOT EXISTS day_historicaldata_eq (
    id VARCHAR(100) NOT NULL,
    instrument_token BIGINT NOT NULL,
    time_stamp DATETIME NOT NULL,
    trading_symbol VARCHAR(50),
    open_price DOUBLE,
    high_price DOUBLE,
    low_price DOUBLE,
    close_price DOUBLE,
    volume BIGINT,
    oi BIGINT DEFAULT 0,
    PRIMARY KEY (id, time_stamp),
    INDEX idx_day_token_time (instrument_token, time_stamp)
) ENGINE=InnoDB
PARTITION BY RANGE (YEAR(time_stamp)) (
    PARTITION p2020 VALUES LESS THAN (2021),
    PARTITION p2021 VALUES LESS THAN (2022),
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ======================================================
-- 6. WEEK TABLE
-- ======================================================
CREATE TABLE IF NOT EXISTS week_historicaldata_eq (
    id VARCHAR(100) NOT NULL,
    instrument_token BIGINT NOT NULL,
    time_stamp DATETIME NOT NULL,
    trading_symbol VARCHAR(50),
    open_price DOUBLE,
    high_price DOUBLE,
    low_price DOUBLE,
    close_price DOUBLE,
    volume BIGINT,
    oi BIGINT DEFAULT 0,
    PRIMARY KEY (id, time_stamp),
    INDEX idx_week_token_time (instrument_token, time_stamp)
) ENGINE=InnoDB
PARTITION BY RANGE (YEAR(time_stamp)) (
    PARTITION p2018 VALUES LESS THAN (2019),
    PARTITION p2019 VALUES LESS THAN (2020),
    PARTITION p2020 VALUES LESS THAN (2021),
    PARTITION p2021 VALUES LESS THAN (2022),
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
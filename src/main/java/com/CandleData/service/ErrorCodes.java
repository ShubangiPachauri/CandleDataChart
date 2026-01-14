package com.CandleData.service;

import lombok.Getter;

@Getter
public enum ErrorCodes {

    // ===== Kite Errors =====
    ERR_KITE_API("ERR_KITE_001", "KITE_API_ERROR",
            "Error occurred while fetching historical data from Kite"),
    ERR_KITE_TOKEN("ERR_KITE_002", "KITE_TOKEN_ERROR",
            "Kite access token missing or expired"),

    // ===== Validation =====
    ERR_VALIDATION("ERR_VAL_001", "VALIDATION_ERROR",
            "Required fields like token or trading symbol are missing"),

    // ===== Database =====
    ERR_DB_SAVE("ERR_DB_001", "DATABASE_SAVE_ERROR",
            "Error occurred while saving data to database"),
    ERR_DB_TABLE("ERR_DB_002", "DATABASE_TABLE_ERROR",
            "Error while creating/accessing historical table"),

    // ===== Scheduler / System =====
    ERR_THREAD_INTERRUPTED("ERR_SYS_001", "THREAD_INTERRUPTED",
            "Scheduler thread interrupted"),
    ERR_DATE_PARSE("ERR_SYS_002", "DATE_PARSE_ERROR",
            "Date parsing failed"),
    ERR_GENERIC("ERR_SYS_999", "GENERAL_ERROR",
            "Unexpected system error");

    private final String code;
    private final String message;
    private final String description;

    ErrorCodes(String code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }
}

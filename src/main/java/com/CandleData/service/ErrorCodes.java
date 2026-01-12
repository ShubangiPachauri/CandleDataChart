package com.CandleData.service;

public enum ErrorCodes {
	ERR01("ERR01", "THIS is default message"),
    ERR02("ERR02", "Another error message");
	
	
	private final String code;
    private final String message;
    
    ErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
	

}

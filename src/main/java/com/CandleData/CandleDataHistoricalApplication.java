package com.CandleData;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CandleDataHistoricalApplication {

	public static void main(String[] args) {
		SpringApplication.run(CandleDataHistoricalApplication.class, args);
	}

}

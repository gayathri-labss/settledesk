package com.settledesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SettleDeskApplication {

	static void main(String[] args) {
		SpringApplication.run(SettleDeskApplication.class, args);
	}
}
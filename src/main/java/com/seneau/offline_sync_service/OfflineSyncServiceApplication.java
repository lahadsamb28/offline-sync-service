package com.seneau.offline_sync_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class OfflineSyncServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OfflineSyncServiceApplication.class, args);
	}

}

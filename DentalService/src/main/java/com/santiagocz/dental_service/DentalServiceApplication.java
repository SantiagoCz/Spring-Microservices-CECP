package com.santiagocz.dental_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class DentalServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DentalServiceApplication.class, args);
	}

}

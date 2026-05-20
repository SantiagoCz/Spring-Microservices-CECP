package com.santiagocz.affiliates_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AffiliatesServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AffiliatesServiceApplication.class, args);
	}

}

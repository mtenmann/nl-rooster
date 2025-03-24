package com.nl.wowapi.boble;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BobleApplication {

	public static void main(String[] args) {
		SpringApplication.run(BobleApplication.class, args);
		System.out.println("Backend kjører på http://localhost:8080");
	}
}

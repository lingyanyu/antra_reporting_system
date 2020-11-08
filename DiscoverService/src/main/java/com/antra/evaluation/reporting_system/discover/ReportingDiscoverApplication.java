package com.antra.evaluation.reporting_system.discover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class ReportingDiscoverApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReportingDiscoverApplication.class, args);
	}
}

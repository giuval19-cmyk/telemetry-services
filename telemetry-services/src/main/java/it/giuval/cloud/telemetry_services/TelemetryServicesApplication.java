package it.giuval.cloud.telemetry_services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TelemetryServicesApplication {

	public static void main(String[] args) {
		SpringApplication.run(TelemetryServicesApplication.class, args);
	}

}
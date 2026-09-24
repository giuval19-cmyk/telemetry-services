package it.giuval.cloud.telemetry_services.domain;

public enum OutboxStatus {

	PENDING,
	PROCESSING,
	SENT,
	FAILED
}

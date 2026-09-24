package it.giuval.cloud.telemetry_services.events;

import java.time.Instant;

import it.giuval.cloud.telemetry_services.domain.DroneStatus;
import it.giuval.cloud.telemetry_services.domain.Position;

public record TelemetryEvent(
		String droneId,
		Position position,
		int batteryLevel,
		DroneStatus status,
		Instant timestamp
		) {

}
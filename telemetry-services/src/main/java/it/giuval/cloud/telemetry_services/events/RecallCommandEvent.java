package it.giuval.cloud.telemetry_services.events;

public record RecallCommandEvent(
		String droneId,
		String reason
	) {

}
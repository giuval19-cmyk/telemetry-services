package it.giuval.cloud.telemetry_services.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type"
		)
@JsonSubTypes({
	@JsonSubTypes.Type(value = DroneCommand.Recall.class, name = "Recall"),
	@JsonSubTypes.Type(value = DroneCommand.Relocate.class, name = "Relocate"),
})
public sealed interface DroneCommand {

	record Recall(String reason) implements DroneCommand {}
	record Relocate(double newLat, double newLon) implements DroneCommand {}
}

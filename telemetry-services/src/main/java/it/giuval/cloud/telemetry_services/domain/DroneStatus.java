package it.giuval.cloud.telemetry_services.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type"
		)
@JsonSubTypes({
	@JsonSubTypes.Type(value = DroneStatus.Active.class, name = "ACTIVE"),
	@JsonSubTypes.Type(value = DroneStatus.LowBattery.class, name = "LOW_BATTERY"),
	@JsonSubTypes.Type(value = DroneStatus.Offline.class, name = "OFFLINE")
})
public sealed interface DroneStatus {

	record Active(String note) implements DroneStatus {}
	record LowBattery(int criticalThreshold) implements DroneStatus {}
	record Offline(String reason) implements DroneStatus {}
}

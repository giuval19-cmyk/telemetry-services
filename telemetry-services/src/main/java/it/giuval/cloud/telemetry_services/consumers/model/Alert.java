package it.giuval.cloud.telemetry_services.consumers.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import it.giuval.cloud.telemetry_services.domain.AlertStatus;
import it.giuval.cloud.telemetry_services.domain.AlertType;
import lombok.Data;

@Document(collection = "alerts")
@Data
public class Alert {

	@Id
	private String id;
	private String droneId;
	private AlertType alertType;
	private AlertStatus status;
	Instant createdAt;
	Instant resolvedAt;
	
	public Alert() {}
	
	public Alert(String droneId, AlertType alertType) {
	    this.droneId = droneId;
        this.alertType = alertType;
        this.status = AlertStatus.OPEN;
        this.createdAt = Instant.now();
	}
}
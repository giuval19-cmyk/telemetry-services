package it.giuval.cloud.telemetry_services.consumers.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import it.giuval.cloud.telemetry_services.domain.OutboxStatus;
import lombok.Data;

@Document(collection = "outbox_messages")
@Data
public class OutboxMessage {

	@Id
	private String id;
	private String aggregateId;
	private String eventType;
	private String payload;
	private OutboxStatus status;
	private Instant createdAt;
	private Instant sentAt;
	private Instant claimedAt;
	private int attempts;
}

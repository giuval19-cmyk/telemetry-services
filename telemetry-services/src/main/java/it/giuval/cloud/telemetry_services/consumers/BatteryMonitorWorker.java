package it.giuval.cloud.telemetry_services.consumers;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.giuval.cloud.telemetry_services.consumers.model.Alert;
import it.giuval.cloud.telemetry_services.consumers.model.OutboxMessage;
import it.giuval.cloud.telemetry_services.domain.AlertType;
import it.giuval.cloud.telemetry_services.domain.OutboxStatus;
import it.giuval.cloud.telemetry_services.events.RecallCommandEvent;
import it.giuval.cloud.telemetry_services.events.TelemetryEvent;

@Service
public class BatteryMonitorWorker implements WorkerConsumer {

	private final Logger logger = LoggerFactory.getLogger(BatteryMonitorWorker.class);

	@Value("${telemetry.battery.critical-threshold}")
	private int criticalThreshold;

	private final MongoTemplate mongoTemplate;
	private final ObjectMapper objectMapper;
	
	public BatteryMonitorWorker(MongoTemplate mongoTemplate, ObjectMapper objectMapper){
		this.mongoTemplate = mongoTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public void process(TelemetryEvent event) {
		int battery = event.batteryLevel();
		if (battery <= criticalThreshold) {

			RecallCommandEvent command = new RecallCommandEvent(event.droneId(), "Battery");
			logger.warn("ALLARME BATTERIA: drone {} al {}% (soglia critica: {}%)",
					event.droneId(), battery, criticalThreshold);
			
			mongoTemplate.save(new Alert(event.droneId(), AlertType.BATTERY_CRITICAL));
			
			var outboxMessagge = new OutboxMessage();
			outboxMessagge.setAggregateId(event.droneId());
			outboxMessagge.setEventType("RecallCommand");
			outboxMessagge.setPayload(serialialize(command));
			outboxMessagge.setStatus(OutboxStatus.PENDING);
			outboxMessagge.setCreatedAt(Instant.now());
			mongoTemplate.save(outboxMessagge);
		}
	}

	private String serialialize(RecallCommandEvent command) {
		try {
			return this.objectMapper.writeValueAsString(command);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Serializzazione comando fallita", e);
		}
	}
}
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
import it.giuval.cloud.telemetry_services.domain.DroneStatus;
import it.giuval.cloud.telemetry_services.domain.OutboxStatus;
import it.giuval.cloud.telemetry_services.events.RecallCommandEvent;
import it.giuval.cloud.telemetry_services.events.TelemetryEvent;
import it.giuval.cloud.telemetry_services.processing.DroneReferencePositions;
import it.giuval.cloud.telemetry_services.util.GeoUtils;

@Service
public class PositionMonitorWorker implements WorkerConsumer{

	private final Logger logger = LoggerFactory.getLogger(PositionMonitorWorker.class);
	private final DroneReferencePositions referencePositions;

	@Value("${telemetry.perimeter.threshold-km}")
	private double perimeterThresholdKm;

	private final MongoTemplate mongoTemplate;
	private final ObjectMapper objectMapper;

	public PositionMonitorWorker(DroneReferencePositions referencePositions, MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
		this.referencePositions = referencePositions;
		this.mongoTemplate = mongoTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public void process(TelemetryEvent event) {

		if (event.status() instanceof DroneStatus.Offline) {
	        logger.info("Drone {} è offline (recall o batteria esaurita). Rimozione dalla mappa attiva.", event.droneId());
	        referencePositions.removePosition(event.droneId());
	        return; // Interrompiamo l'elaborazione per questo evento
	    }
		
		var reference = referencePositions.getOrSetReference(event.droneId(), event.position());

		referencePositions.updatePositions(event.droneId(), event.position());
		
		double distanceKm = GeoUtils.distanceKm(reference, event.position());
		if (distanceKm > perimeterThresholdKm) {
			logger.warn("ALLARME PERIMETRO: drone {} a {} km dalla zona operativa (soglia: {} km)",
					event.droneId(), String.format("%.2f", distanceKm), perimeterThresholdKm);

			RecallCommandEvent command = new RecallCommandEvent(event.droneId(), "Out of perimetry");

			mongoTemplate.save(new Alert(event.droneId(), AlertType.PERIMETER_EXCEEDED));

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
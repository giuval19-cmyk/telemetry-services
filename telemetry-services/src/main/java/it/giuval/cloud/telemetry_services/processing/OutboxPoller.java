package it.giuval.cloud.telemetry_services.processing;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import it.giuval.cloud.telemetry_services.consumers.model.OutboxMessage;
import it.giuval.cloud.telemetry_services.domain.OutboxStatus;
import it.giuval.cloud.telemetry_services.events.RecallCommandEvent;

@Component
public class OutboxPoller {

	private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 20;
    private static final long STUCK_THRESHOLD_SECONDS = 30;
    
	private final CommandPublisher publisher;
	private final MongoTemplate mongoTemplate;
	private final ObjectMapper objectMapper;
	private final CircuitBreaker commandPublisherCircuitBreaker;

	private final Logger logger = LoggerFactory.getLogger(OutboxPoller.class);

	public OutboxPoller(CommandPublisher publisher, MongoTemplate mongoTemplate, ObjectMapper objectMapper,
			CircuitBreakerRegistry circuitBreakerRegistry) {
		
		this.publisher = publisher;
		this.mongoTemplate = mongoTemplate;
		this.objectMapper = objectMapper;
		//Istanza del Circuit Breaker definita su CommandPublisher
		this.commandPublisherCircuitBreaker = circuitBreakerRegistry.circuitBreaker("commandPublisher");
	}

	@Scheduled(fixedDelay = 1000)
	public void processOutboxEvents() {
		int count = 0;

		// Se il Circuit Breaker è APERTO, sospendiamo il polling per non consumare tentativi inutilmente
		if (commandPublisherCircuitBreaker.getState() == CircuitBreaker.State.OPEN) {
			logger.warn("Circuit Breaker 'commandPublisher' è OPEN. Polling sospeso temporaneamente.");
			return;
		}

		// Definiamo la query con ordinamento FIFO (dal più vecchio al più recente)
		Query query = Query.query(Criteria.where("status").is(OutboxStatus.PENDING))
				.with(org.springframework.data.domain.Sort.by(
						org.springframework.data.domain.Sort.Direction.ASC, "createdAt"
						));

		while(count<BATCH_SIZE) {
			//Recupera da mongo
			OutboxMessage claimed = mongoTemplate.findAndModify(
					query,
					Update.update("status", OutboxStatus.PROCESSING).set("claimedAt", Instant.now()),
					OutboxMessage.class
					);
			if(claimed==null) {
				break;
			}

			tryPublish(claimed);

			count++;
		}
	}

	//Reset di eventuali messaggi rimasti in processing
	@Scheduled(fixedDelay = 3000)
	private void recoverStuckMessages() {
		Instant cutoff = Instant.now().minusSeconds(STUCK_THRESHOLD_SECONDS);//aggiorno solo quelli bloccati oltre i 30 secondi
		var result = mongoTemplate.updateMulti(
				Query.query(Criteria.where("status").is(OutboxStatus.PROCESSING).and("claimedAt").lt(cutoff)),
				Update.update("status", OutboxStatus.PENDING).inc("attempts",1), 
				OutboxMessage.class);

		if(result.getModifiedCount()>0) {
			logger.warn("Recuperati {} messaggi bloccati in PROCESSING", result.getModifiedCount());
		}
	}
	
	// Esegue il job ogni 3 ore
    @Scheduled(fixedDelay = 10_800_000)
    private void retryFailed() {
		Query query = Query.query(Criteria.where("status").is(OutboxStatus.FAILED));
		
		UpdateResult result = mongoTemplate.updateMulti(
				query,
				Update.update("status", OutboxStatus.PENDING).set("attempts", 0),
				OutboxMessage.class);
		
		logger.warn("Recuperati {} messaggi bloccati in FAILED", result.getModifiedCount());
    }

	private void tryPublish(OutboxMessage message) {

		try {
			publisher.publishRecall(deserialize(message.getPayload()), message.getId());
			// Se l'invio ha successo, aggiorna il documento su Mongo
			mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(message.getId())),
					Update.update("status", OutboxStatus.SENT).set("sentAt", Instant.now()), 
					OutboxMessage.class);
		}
		catch(Exception e) {

			logger.error("Fallita pubblicazione outbox message {}: {}", message.getId(), e.getMessage());

			int currentAttempts = message.getAttempts() + 1;

			if (currentAttempts >= MAX_ATTEMPTS) {
				//DLQ
				mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(message.getId())),
						Update.update("status", OutboxStatus.FAILED).inc("attempts", 1), 
						OutboxMessage.class);
			}
			else {
				//Retry: riporta a Pending incrementando i tentativi
				mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(message.getId())),
						Update.update("status", OutboxStatus.PENDING).inc("attempts", 1), 
						OutboxMessage.class);
			}
		}
	}

	private RecallCommandEvent deserialize(String payload) {
		try {
			return objectMapper.readValue(payload, RecallCommandEvent.class);
		} catch (Exception e) {
			throw new RuntimeException("Deserializzazione payload outbox fallita", e);
		}
	}
}
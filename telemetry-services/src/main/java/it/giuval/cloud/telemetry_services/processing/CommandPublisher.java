package it.giuval.cloud.telemetry_services.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import it.giuval.cloud.telemetry_services.events.RecallCommandEvent;
import it.giuval.cloud.telemetry_services.exceptions.CommandPublishException;

@Component
public class CommandPublisher {

	private final Logger logger = LoggerFactory.getLogger(CommandPublisher.class);

	@Value("${telemetry.rabbitmq.exchange}")
	private String COMMAND_EXCHANGE;
	@Value("${telemetry.rabbitmq.routing-key}")
	private String COMMAND_ROUTING_KEY;

	private final RabbitTemplate rabbitTemplate;

	public CommandPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@CircuitBreaker(name = "commandPublisher", fallbackMethod = "fallbackPublishRecall")
	public void publishRecall(RecallCommandEvent event, String outboxId) {

		rabbitTemplate.invoke(operations->{
			operations.convertAndSend(COMMAND_EXCHANGE, COMMAND_ROUTING_KEY,
					event, message ->{
						message.getMessageProperties().setCorrelationId(outboxId);
						message.getMessageProperties().setHeader("outbox_id", outboxId);
						return message;
					});
			return operations.waitForConfirms(1000);
		});
	}

	protected void fallbackPublishRecall(RecallCommandEvent event, String outboxId, Throwable t) {
		logger.error(">> FALLBACK <<: Impossibile pubblicare Recall per drone {}: {}. Fallback attivato.",
				event.droneId(), t.getMessage());

		throw new CommandPublishException("Impossibile pubblicare Recall per drone " + event.droneId(), t);
	}
}
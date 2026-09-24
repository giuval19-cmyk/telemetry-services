package it.giuval.cloud.telemetry_services.controller;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.giuval.cloud.telemetry_services.domain.Position;
import it.giuval.cloud.telemetry_services.processing.DroneReferencePositions;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class DroneServicesController {

	private final int MAX_GRACE_SECONDS = 5; // 5 secondi di tolleranza massima all'avvio
	private final Logger log = LoggerFactory.getLogger(DroneServicesController.class);
	@Autowired
	private DroneReferencePositions dronePositions;

	@GetMapping(value = "/telemetry/{droneId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<Position> telemetry(@PathVariable String droneId) {

		// Stato locale isolato per questa specifica connessione SSE
		final boolean[] hasStarted = {false};
		final int[] startupGraceCounter = {0};
		final boolean[] sentEndSignal = {false}; //Tracciamo se abbiamo già inviato la posizione di atterraggio al client

		log.info("Drone {} connesso al canale di notifiche SSE!",droneId);

		return Flux.interval(Duration.ofSeconds(1))
				.handle((tick, sink) -> {
					Position position = dronePositions.getPositions(droneId);
					log.debug("Controllo posizione per {}: {}", droneId, position);
					if (position != null) {
						// Il drone sta trasmettendo regolarmente
						hasStarted[0] = true;
						sink.next(position);
					}
					else {
						//Se non troviamo la posizione, potremmo essere in fase di avvio
						if (!hasStarted[0]) {
							//Non ha iniziato a volare, aspettiamo un pò prima di chiudere
							startupGraceCounter[0]++;
							if (startupGraceCounter[0] >= MAX_GRACE_SECONDS) {
								log.info("Timeout avvio per il drone {}. Chiusura stream SSE.", droneId);
								sink.complete();
							}
						}
						else {
							// Aveva iniziato a volare, ma ora è null (atterrato)
							if (!sentEndSignal[0]) {
								sentEndSignal[0] = true;
								log.info("Drone {} atterrato. Invio segnale di atterraggio.", droneId);
								sink.next(new Position(0, 0, 0));
							} else {
								//Al ciclo successivo (dopo un secondo), chiudiamo lo stream in sicurezza
								sink.complete();
							}
						}
					}
				});
	}
}
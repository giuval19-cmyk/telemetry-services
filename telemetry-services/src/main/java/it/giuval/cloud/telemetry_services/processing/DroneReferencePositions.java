package it.giuval.cloud.telemetry_services.processing;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import it.giuval.cloud.telemetry_services.domain.Position;

@Component
public class DroneReferencePositions {
	//Mantengo la prima posizione osservata per ogni drone
	private final ConcurrentHashMap<String, Position> referencePositions = new ConcurrentHashMap<>();
	
	private final ConcurrentHashMap<String, Position> latestPositions = new ConcurrentHashMap<String, Position>();

	public Position getOrSetReference(String droneId, Position currentPosition) {
		return referencePositions.computeIfAbsent(droneId, id -> currentPosition);
	}
	
	public void updatePositions(String droneId, Position currentPosition) {
		latestPositions.put(droneId, currentPosition);
	}
	
	public Position getPositions(String droneId) {
		return latestPositions.get(droneId);
	}

	public void removePosition(String droneId) {
		latestPositions.remove(droneId);
	    referencePositions.remove(droneId);		
	}
}
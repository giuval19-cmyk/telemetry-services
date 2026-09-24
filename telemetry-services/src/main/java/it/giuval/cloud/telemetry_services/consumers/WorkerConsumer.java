package it.giuval.cloud.telemetry_services.consumers;

import it.giuval.cloud.telemetry_services.events.TelemetryEvent;

public interface WorkerConsumer {

	public void process(TelemetryEvent event);
}

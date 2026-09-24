package it.giuval.cloud.telemetry_services.consumers;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import it.giuval.cloud.telemetry_services.events.TelemetryEvent;

@Component
public class TelemetryListener {

	private final PositionMonitorWorker positionWorker;
	private final BatteryMonitorWorker batteryWorker;
	
	public TelemetryListener(PositionMonitorWorker positionWorker, BatteryMonitorWorker batteryWorker) {
		this.positionWorker = positionWorker;
		this.batteryWorker = batteryWorker;
	}
	
    @RabbitListener(queues = "${telemetry.rabbitmq.queue}")
    public void onTelemetry(TelemetryEvent event) {
    	Thread.startVirtualThread(()-> positionWorker.process(event));
    	Thread.startVirtualThread(()-> batteryWorker.process(event));
    }
}

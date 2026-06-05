package br.com.cmachado.parkingsystem.application.garage.impl;

import br.com.cmachado.parkingsystem.domain.model.sector.events.GarageAtCapacity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Emits an observability metric whenever a vehicle enters while the garage is at capacity.
 * Visible at {@code /actuator/metrics/garage.entry.at_capacity} and exported to any
 * configured OTel/Prometheus endpoint.
 */
@Component
public class GarageCapacityMetricsHandler {

    private final Counter garageFullEntryCounter;

    public GarageCapacityMetricsHandler(MeterRegistry registry) {
        this.garageFullEntryCounter = Counter.builder("garage.entry.at_capacity")
                .description("Entries recorded while garage was at full capacity")
                .register(registry);
    }

    @EventListener
    public void on(GarageAtCapacity event) {
        garageFullEntryCounter.increment();
    }
}

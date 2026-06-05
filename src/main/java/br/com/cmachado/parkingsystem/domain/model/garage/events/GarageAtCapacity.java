package br.com.cmachado.parkingsystem.domain.model.garage.events;

import br.com.cmachado.parkingsystem.domain.model.vehicle.LicensePlate;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Published when a vehicle entry is recorded while the garage is at full capacity.
 * Consumed by observability handlers to emit metrics — does not block the entry.
 */
@Getter
public class GarageAtCapacity {

    private final LicensePlate licensePlate;
    private final LocalDateTime occurredAt;

    public GarageAtCapacity(LicensePlate licensePlate, LocalDateTime occurredAt) {
        this.licensePlate = licensePlate;
        this.occurredAt = occurredAt;
    }
}

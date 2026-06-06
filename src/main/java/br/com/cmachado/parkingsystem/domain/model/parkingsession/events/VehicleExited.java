package br.com.cmachado.parkingsystem.domain.model.parkingsession.events;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

/**
 * Raised when a vehicle exits the garage. Carries the sector, exit date and amount charged
 * so the daily revenue can be updated asynchronously after the exit transaction commits.
 */
@Getter
public class VehicleExited extends DomainEvent {

    private final ParkingSession session;

    public VehicleExited(ParkingSession session) {
        super(session);
        this.session = session;
    }
}

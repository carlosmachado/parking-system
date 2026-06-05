package br.com.cmachado.parkingsystem.domain.model.vehicle.events;

import br.com.cmachado.parkingsystem.domain.model.vehicle.ParkingSession;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class VehicleParked extends DomainEvent {

    private final ParkingSession session;

    public VehicleParked(ParkingSession session) {
        super(session);
        this.session = session;
    }
}

package br.com.cmachado.parkingsystem.domain.model.vehicle.events;

import br.com.cmachado.parkingsystem.domain.model.vehicle.ParkingSession;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class VehicleEntered extends DomainEvent {

    private final ParkingSession session;

    public VehicleEntered(ParkingSession session) {
        super(session);
        this.session = session;
    }
}

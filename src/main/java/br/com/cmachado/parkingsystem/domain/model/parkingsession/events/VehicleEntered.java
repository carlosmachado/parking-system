package br.com.cmachado.parkingsystem.domain.model.parkingsession.events;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
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

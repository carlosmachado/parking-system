package br.com.cmachado.parkingsystem.domain.model.parkingspot.events;

import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class SpotReleased extends DomainEvent {

    private final ParkingSpot spot;

    public SpotReleased(ParkingSpot spot) {
        super(spot);
        this.spot = spot;
    }
}

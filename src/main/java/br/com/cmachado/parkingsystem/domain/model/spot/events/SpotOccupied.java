package br.com.cmachado.parkingsystem.domain.model.spot.events;

import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class SpotOccupied extends DomainEvent {

    private final ParkingSpot spot;

    public SpotOccupied(ParkingSpot spot) {
        super(spot);
        this.spot = spot;
    }
}

package br.com.cmachado.parkingsystem.domain.model.parkingspot.events;

import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class ParkingSpotOccupied extends DomainEvent {

    private final ParkingSpot spot;

    public ParkingSpotOccupied(ParkingSpot spot) {
        super(spot);
        this.spot = spot;
    }
}

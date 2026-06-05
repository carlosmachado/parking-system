package br.com.cmachado.parkingsystem.domain.model.spot.events;

import br.com.cmachado.parkingsystem.domain.model.spot.Spot;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class SpotOccupied extends DomainEvent {

    private final Spot spot;

    public SpotOccupied(Spot spot) {
        super(spot);
        this.spot = spot;
    }
}

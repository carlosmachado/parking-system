package br.com.cmachado.parkingsystem.domain.model.garage.events;

import br.com.cmachado.parkingsystem.domain.model.garage.Sector;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class SectorCreated extends DomainEvent {

    private final Sector sector;

    public SectorCreated(Sector sector) {
        super(sector);
        this.sector = sector;
    }
}

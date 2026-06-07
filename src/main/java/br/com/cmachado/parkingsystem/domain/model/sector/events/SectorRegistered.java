package br.com.cmachado.parkingsystem.domain.model.sector.events;

import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class SectorRegistered extends DomainEvent {

    private final Sector sector;

    public SectorRegistered(Sector sector) {
        super(sector);
        this.sector = sector;
    }
}

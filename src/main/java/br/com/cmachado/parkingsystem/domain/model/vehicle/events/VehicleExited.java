package br.com.cmachado.parkingsystem.domain.model.vehicle.events;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.vehicle.ParkingSession;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Raised when a vehicle exits the garage. Carries the sector, exit date and amount charged
 * so the daily revenue can be updated asynchronously after the exit transaction commits.
 */
@Getter
public class VehicleExited extends DomainEvent {

    private final ParkingSession session;
    private final SectorCode sectorCode;
    private final LocalDate exitDate;
    private final Money amountCharged;

    public VehicleExited(ParkingSession session, SectorCode sectorCode, LocalDate exitDate, Money amountCharged) {
        super(session);
        this.session = session;
        this.sectorCode = sectorCode;
        this.exitDate = exitDate;
        this.amountCharged = amountCharged;
    }
}

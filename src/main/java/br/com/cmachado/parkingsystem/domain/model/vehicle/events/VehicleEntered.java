package br.com.cmachado.parkingsystem.domain.model.vehicle.events;

import br.com.cmachado.parkingsystem.domain.model.vehicle.VehicleEvent;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class VehicleEntered extends DomainEvent {

    private final VehicleEvent vehicleEvent;

    public VehicleEntered(VehicleEvent vehicleEvent) {
        super(vehicleEvent);
        this.vehicleEvent = vehicleEvent;
    }
}

package br.com.cmachado.parkingsystem.domain.model.parkingspot.violations;

import br.com.cmachado.parkingsystem.infrastructure.http.NotFoundException;
import br.com.cmachado.parkingsystem.infrastructure.http.ErrorCodes;

public class ParkingSpotNotFoundException extends NotFoundException {

    public ParkingSpotNotFoundException(String message) {
        super(message, ErrorCodes.PARKING_SPOT_NOT_FOUND);
    }
}

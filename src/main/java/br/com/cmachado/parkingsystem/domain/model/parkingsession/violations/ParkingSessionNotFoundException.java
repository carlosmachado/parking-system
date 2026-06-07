package br.com.cmachado.parkingsystem.domain.model.parkingsession.violations;

import br.com.cmachado.parkingsystem.infrastructure.http.NotFoundException;
import br.com.cmachado.parkingsystem.infrastructure.http.ErrorCodes;

public class ParkingSessionNotFoundException extends NotFoundException {

    public ParkingSessionNotFoundException(String message) {
        super(message, ErrorCodes.PARKING_SESSION_NOT_FOUND);
    }
}

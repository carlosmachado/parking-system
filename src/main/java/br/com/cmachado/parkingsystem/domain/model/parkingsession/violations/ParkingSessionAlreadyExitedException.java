package br.com.cmachado.parkingsystem.domain.model.parkingsession.violations;

import br.com.cmachado.parkingsystem.infrastructure.http.CodedException;
import br.com.cmachado.parkingsystem.infrastructure.http.ErrorCodes;

public class ParkingSessionAlreadyExitedException extends RuntimeException implements CodedException {

    public ParkingSessionAlreadyExitedException() {
        super("Parking session has already exited");
    }

    @Override
    public String getCode() {
        return ErrorCodes.PARKING_SESSION_ALREADY_EXITED;
    }
}

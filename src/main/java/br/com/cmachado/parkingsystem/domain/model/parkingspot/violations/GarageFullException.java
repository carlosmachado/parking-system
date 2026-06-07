package br.com.cmachado.parkingsystem.domain.model.parkingspot.violations;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.infrastructure.http.CodedException;
import br.com.cmachado.parkingsystem.infrastructure.http.ErrorCodes;

public class GarageFullException extends RuntimeException implements CodedException {

    public GarageFullException(LicensePlate licensePlate) {
        super("Garage is full; entry was blocked for plate %s".formatted(licensePlate));
    }

    @Override
    public String getCode() {
        return ErrorCodes.GARAGE_FULL;
    }
}

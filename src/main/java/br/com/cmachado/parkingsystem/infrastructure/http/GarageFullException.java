package br.com.cmachado.parkingsystem.infrastructure.http;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;

public class GarageFullException extends RuntimeException {

    public static final String CODE = "EST-001";

    public GarageFullException(LicensePlate licensePlate) {
        super("Parking is full");
    }
}

package br.com.cmachado.parkingsystem.domain.model.spot;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;

public class GarageFullException extends RuntimeException {

    public static final String CODE = "EST-001";

    public GarageFullException(LicensePlate licensePlate) {
        super("Garage is full. Just blocked: " + licensePlate);
    }
}

package br.com.cmachado.parkingsystem.domain.model.parkingspot;

import br.com.cmachado.parkingsystem.infrastructure.http.NotFoundException;

public class ParkingSpotNotFoundException extends NotFoundException {
    public ParkingSpotNotFoundException(String message) {
        super(message);
    }
}

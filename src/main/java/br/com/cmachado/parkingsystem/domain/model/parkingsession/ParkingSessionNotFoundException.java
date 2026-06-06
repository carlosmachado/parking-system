package br.com.cmachado.parkingsystem.domain.model.parkingsession;

import br.com.cmachado.parkingsystem.infrastructure.http.NotFoundException;

public class ParkingSessionNotFoundException extends NotFoundException {
    public ParkingSessionNotFoundException(String message) {
        super(message);
    }
}

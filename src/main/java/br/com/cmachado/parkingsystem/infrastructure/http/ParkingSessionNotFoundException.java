package br.com.cmachado.parkingsystem.infrastructure.http;

public class ParkingSessionNotFoundException extends NotFoundException {
    public ParkingSessionNotFoundException(String message) {
        super(message);
    }
}

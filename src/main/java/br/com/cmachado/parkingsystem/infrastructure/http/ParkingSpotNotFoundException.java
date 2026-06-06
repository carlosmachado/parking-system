package br.com.cmachado.parkingsystem.infrastructure.http;

public class ParkingSpotNotFoundException extends NotFoundException {
    public ParkingSpotNotFoundException(String message) {
        super(message);
    }
}

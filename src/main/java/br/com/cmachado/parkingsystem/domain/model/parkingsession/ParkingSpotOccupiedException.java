package br.com.cmachado.parkingsystem.domain.model.parkingsession;

public class ParkingSpotOccupiedException extends RuntimeException {
    public ParkingSpotOccupiedException(String message) {
        super(message);
    }
}

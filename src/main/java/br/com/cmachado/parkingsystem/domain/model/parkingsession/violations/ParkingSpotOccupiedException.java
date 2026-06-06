package br.com.cmachado.parkingsystem.domain.model.parkingsession.violations;

public class ParkingSpotOccupiedException extends RuntimeException {
    public ParkingSpotOccupiedException(String message) {
        super(message);
    }
}

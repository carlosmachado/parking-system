package br.com.cmachado.parkingsystem.infrastructure.http;

public class GarageFullException extends RuntimeException {

    public static final String CODE = "EST-001";

    public GarageFullException() {
        super("Parking is full");
    }
}

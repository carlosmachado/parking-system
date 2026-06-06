package br.com.cmachado.parkingsystem.domain.model.parkingsession;

public class CantParkSessionException extends RuntimeException {
    public CantParkSessionException(String message) {
        super(message);
    }
}

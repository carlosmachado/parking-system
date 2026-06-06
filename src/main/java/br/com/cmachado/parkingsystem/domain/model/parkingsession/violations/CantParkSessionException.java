package br.com.cmachado.parkingsystem.domain.model.parkingsession.violations;

public class CantParkSessionException extends RuntimeException {
    public CantParkSessionException(String message) {
        super(message);
    }
}

package br.com.cmachado.parkingsystem.domain.model.common.money;

public class MoneyInvalidException extends RuntimeException {
    public MoneyInvalidException(String message) {
        super(message);
    }
}

package br.com.cmachado.parkingsystem.domain.model.common.money;

public class MoneyInvalidException extends IllegalArgumentException {
    public MoneyInvalidException(String message) {
        super(message);
    }
}

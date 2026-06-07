package br.com.cmachado.parkingsystem.domain.model.parkingsession.violations;

import br.com.cmachado.parkingsystem.infrastructure.http.CodedException;
import br.com.cmachado.parkingsystem.infrastructure.http.ErrorCodes;

public class CantParkSessionException extends RuntimeException implements CodedException {

    public CantParkSessionException(String message) {
        super(message);
    }

    @Override
    public String getCode() {
        return ErrorCodes.INVALID_PARKING_SESSION_STATE;
    }
}

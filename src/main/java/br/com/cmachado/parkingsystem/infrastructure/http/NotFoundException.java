package br.com.cmachado.parkingsystem.infrastructure.http;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException implements CodedException {

    private final String code;

    public NotFoundException(String message) {
        this(message, ErrorCodes.PARKING_SESSION_NOT_FOUND);
    }

    public NotFoundException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

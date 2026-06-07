package br.com.cmachado.parkingsystem.infrastructure.http;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException implements CodedException {

    private final String code;

    public BadRequestException(String message) {
        this(message, ErrorCodes.REQUEST_VALIDATION);
    }

    public BadRequestException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

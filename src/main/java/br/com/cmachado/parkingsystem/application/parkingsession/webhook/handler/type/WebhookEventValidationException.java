package br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type;

import br.com.cmachado.parkingsystem.infrastructure.http.BadRequestException;

public class WebhookEventValidationException extends BadRequestException {

    public WebhookEventValidationException(String message) {
        super(message);
    }
}

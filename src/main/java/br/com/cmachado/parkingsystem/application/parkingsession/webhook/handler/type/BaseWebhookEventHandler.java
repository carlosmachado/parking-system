package br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventHandler;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public abstract class BaseWebhookEventHandler implements WebhookEventHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public final void handle(WebhookEventRequest request) {
        validate(request);
        doHandle(request);
    }

    protected abstract void validate(WebhookEventRequest request);

    protected abstract void doHandle(WebhookEventRequest request);

    protected LocalDateTime parseTimestamp(String field, String value) {
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new WebhookEventValidationException("%s has invalid ISO date-time value".formatted(field));
        }
    }
}

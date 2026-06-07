package br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler;

import br.com.cmachado.parkingsystem.infrastructure.http.BadRequestException;

import java.util.Locale;

public enum WebhookEventType {
    ENTRY,
    PARKED,
    EXIT;

    public static WebhookEventType from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("event_type is required");
        }

        try {
            return WebhookEventType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unrecognized event_type '%s'".formatted(raw));
        }
    }
}

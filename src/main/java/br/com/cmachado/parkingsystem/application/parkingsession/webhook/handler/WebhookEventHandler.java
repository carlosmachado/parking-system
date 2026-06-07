package br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler;

import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;

public interface WebhookEventHandler {

    WebhookEventType eventType();

    void handle(WebhookEventRequest request);
}

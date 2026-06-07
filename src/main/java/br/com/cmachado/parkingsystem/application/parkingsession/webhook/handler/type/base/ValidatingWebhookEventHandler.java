package br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type.base;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventHandler;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;

public abstract class ValidatingWebhookEventHandler implements WebhookEventHandler {

    @Override
    public final void handle(WebhookEventRequest request) {
        validate(request);
        doHandle(request);
    }

    protected abstract void validate(WebhookEventRequest request);

    protected abstract void doHandle(WebhookEventRequest request);
}

package br.com.cmachado.parkingsystem.application.parkingsession.webhook.mediator;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.resolver.WebhookEventHandlerResolver;
import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventType;
import br.com.cmachado.parkingsystem.infrastructure.http.BadRequestException;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookEventMediator {

    private static final Logger logger = LoggerFactory.getLogger(WebhookEventMediator.class);

    private final WebhookEventHandlerResolver handlerResolver;

    public WebhookEventMediator(WebhookEventHandlerResolver handlerResolver) {
        this.handlerResolver = handlerResolver;
    }

    @Transactional
    public void handle(WebhookEventRequest request) {
        if (request == null)
            throw new BadRequestException("request body is required");

        if (request.getLicensePlate() == null || request.getLicensePlate().isBlank())
            throw new BadRequestException("license_plate is required");

        var eventType = WebhookEventType.from(request.getEventType());
        logger.debug("Dispatching {} webhook: plate={}", eventType, request.getLicensePlate());

        var handler = handlerResolver.resolve(eventType);
        handler.handle(request);
    }
}

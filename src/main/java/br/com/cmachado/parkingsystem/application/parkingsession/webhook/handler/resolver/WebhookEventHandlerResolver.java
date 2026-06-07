package br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.resolver;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventHandler;
import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class WebhookEventHandlerResolver {

    private final Map<WebhookEventType, WebhookEventHandler> handlers;

    public WebhookEventHandlerResolver(List<WebhookEventHandler> handlers) {
        this.handlers = new EnumMap<>(WebhookEventType.class);
        handlers.forEach(handler -> this.handlers.put(handler.eventType(), handler));
    }

    public WebhookEventHandler resolve(WebhookEventType eventType) {
        WebhookEventHandler handler = handlers.get(eventType);
        if (handler == null) {
            throw new IllegalStateException("No webhook event handler registered for " + eventType);
        }
        return handler;
    }
}

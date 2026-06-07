package br.com.cmachado.parkingsystem.application.parkingsession;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventHandler;
import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.resolver.WebhookEventHandlerResolver;
import br.com.cmachado.parkingsystem.application.parkingsession.webhook.mediator.WebhookEventMediator;
import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventType;
import br.com.cmachado.parkingsystem.fixtures.WebhookEventFixture;
import br.com.cmachado.parkingsystem.infrastructure.http.BadRequestException;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookEventMediatorTest {

    private static final LocalDateTime ENTRY = LocalDateTime.parse("2025-01-01T10:00:00");

    @Mock private WebhookEventHandlerResolver handlerResolver;
    @Mock private WebhookEventHandler handler;

    private WebhookEventMediator mediator;

    @BeforeEach
    void setUp() {
        mediator = new WebhookEventMediator(handlerResolver);
    }

    @Test
    void missingLicensePlateThrows400() {
        WebhookEventRequest request = WebhookEventFixture.anEntry().withPlate(null).at(ENTRY).build();

        assertThrows(BadRequestException.class, () -> mediator.handle(request),
                "missing license plate must be rejected");
        verifyNoInteractions(handlerResolver);
    }

    @Test
    void missingEventTypeThrows400() {
        WebhookEventRequest request = WebhookEventFixture.ofType(null).withPlate("ZUL0001").build();

        assertThrows(BadRequestException.class, () -> mediator.handle(request),
                "missing event type must be rejected");
        verifyNoInteractions(handlerResolver);
    }

    @Test
    void unknownEventTypeThrows400() {
        WebhookEventRequest request = WebhookEventFixture.ofType("TELEPORT").withPlate("ZUL0001").build();

        assertThrows(BadRequestException.class, () -> mediator.handle(request),
                "unknown event type must be rejected");
        verifyNoInteractions(handlerResolver);
    }

    @Test
    void delegatesToResolvedHandler() {
        WebhookEventRequest request = WebhookEventFixture.anEntry().withPlate("ZUL0001").at(ENTRY).build();
        when(handlerResolver.resolve(WebhookEventType.ENTRY)).thenReturn(handler);

        mediator.handle(request);

        verify(handlerResolver).resolve(WebhookEventType.ENTRY);
        verify(handler).handle(request);
    }
}

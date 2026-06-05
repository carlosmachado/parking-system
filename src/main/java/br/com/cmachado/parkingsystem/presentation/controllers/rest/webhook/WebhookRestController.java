package br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook;

import br.com.cmachado.parkingsystem.application.webhook.WebhookApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives the simulator's parking events and dispatches each to the application service.
 * Always answers HTTP 200 on success, as expected by the simulator.
 */
@RestController
@RequestMapping("/webhook")
public class WebhookRestController {

    private final WebhookApplicationService webhookService;

    public WebhookRestController(WebhookApplicationService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Routes an ENTRY, PARKED or EXIT event to the matching use case.
     *
     * @throws IllegalArgumentException if {@code event_type} is missing or unknown
     */
    @PostMapping
    public ResponseEntity<Void> handleEvent(@RequestBody WebhookEventRequest request) {
        if (request.getEventType() == null) {
            throw new IllegalArgumentException("event_type is required");
        }

        switch (request.getEventType().toUpperCase()) {
            case "ENTRY" -> webhookService.processEntry(request);
            case "PARKED" -> webhookService.processParked(request);
            case "EXIT" -> webhookService.processExit(request);
            default -> throw new IllegalArgumentException("Unknown event_type: " + request.getEventType());
        }

        return ResponseEntity.ok().build();
    }
}

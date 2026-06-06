package br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook;

import br.com.cmachado.parkingsystem.application.webhook.ParkingSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    /** Attempts for a use case that may lose an optimistic-lock race on a Spot. */
    private static final int MAX_ATTEMPTS = 3;

    private final ParkingSessionService parkingSessionService;

    public WebhookRestController(ParkingSessionService parkingSessionService) {
        this.parkingSessionService = parkingSessionService;
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
            case "ENTRY" -> runWithRetry(() -> parkingSessionService.processEntry(request));
            case "PARKED" -> runWithRetry(() -> parkingSessionService.processParked(request));
            case "EXIT" -> runWithRetry(() -> parkingSessionService.processExit(request));
            default -> throw new IllegalArgumentException("Unknown event_type: " + request.getEventType());
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Runs a use case, retrying when a concurrent request wins an optimistic-lock race on a
     * shared aggregate (e.g. two PARKED events targeting the same spot). Each invocation runs
     * in its own transaction, so a retry re-reads fresh state and picks the next free spot.
     */
    private void runWithRetry(Runnable useCase) {
        for (int attempt = 1; ; attempt++) {
            try {
                useCase.run();
                return;
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw ex;
                }
            }
        }
    }
}

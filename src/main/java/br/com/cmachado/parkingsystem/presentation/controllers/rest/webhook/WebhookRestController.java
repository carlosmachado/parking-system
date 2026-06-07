package br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.mediator.WebhookEventMediator;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives the simulator's parking events and delegates to {@link WebhookEventMediator}.
 * Always answers HTTP 200 on success, as expected by the simulator.
 */
@RestController
@RequestMapping("/webhook")
public class WebhookRestController {

    private static final int MAX_ATTEMPTS = 3;

    private final WebhookEventMediator webhookEventMediator;

    public WebhookRestController(WebhookEventMediator webhookEventMediator) {
        this.webhookEventMediator = webhookEventMediator;
    }

    @PostMapping
    public ResponseEntity<Void> handleEvent(@RequestBody WebhookEventRequest request) {
        runWithRetry(() -> webhookEventMediator.handle(request));
        return ResponseEntity.ok().build();
    }

    /**
     * Retries when a concurrent request wins an optimistic-lock race on a shared aggregate.
     * Each invocation runs in its own transaction so a retry re-reads fresh state.
     */
    private void runWithRetry(Runnable useCase) {
        for (int attempt = 1; ; attempt++) {
            try {
                useCase.run();
                return;
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt >= MAX_ATTEMPTS) throw ex;
            }
        }
    }
}

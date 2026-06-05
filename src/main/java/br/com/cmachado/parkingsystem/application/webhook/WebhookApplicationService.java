package br.com.cmachado.parkingsystem.application.webhook;

import br.com.cmachado.parkingsystem.domain.shared.ApplicationService;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public interface WebhookApplicationService {

    @Transactional
    void processEntry(WebhookEventRequest request);

    @Transactional
    void processParked(WebhookEventRequest request);

    @Transactional
    void processExit(WebhookEventRequest request);
}

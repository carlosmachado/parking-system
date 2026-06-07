package br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventType;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.ParkingSessionNotFoundException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotId;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.violations.ParkingSpotNotFoundException;
import br.com.cmachado.parkingsystem.domain.service.pricing.ChargeCalculator;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ExitWebhookEventHandler extends BaseWebhookEventHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final ParkingSessionRepository sessionRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final ChargeCalculator chargeCalculator;

    public ExitWebhookEventHandler(ParkingSessionRepository sessionRepository,
                                   ParkingSpotRepository parkingSpotRepository,
                                   ChargeCalculator chargeCalculator) {
        this.sessionRepository = sessionRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.chargeCalculator = chargeCalculator;
    }

    @Override
    public WebhookEventType eventType() {
        return WebhookEventType.EXIT;
    }

    @Override
    protected void validate(WebhookEventRequest request) {
        if (request.getExitTime() == null) {
            throw new WebhookEventValidationException("exit_time is required for EXIT events");
        }
    }

    @Override
    protected void doHandle(WebhookEventRequest request) {
        var licensePlate = LicensePlate.of(request.getLicensePlate());
        var exitTime = LocalDateTime.parse(request.getExitTime(), FORMATTER);

        ParkingSession session = sessionRepository.findByLicensePlateAndStatusIn(
                        licensePlate, List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED))
                .orElseThrow(() -> new ParkingSessionNotFoundException("No active session found for plate " + licensePlate));

        if (session.isParked())
            tryToReleaseSpot(session);

        chargeCalculator.charge(session, exitTime);
        sessionRepository.save(session);
    }

    private void tryToReleaseSpot(ParkingSession session) {
        ParkingSpotId spotId = session.getSpotId();
        ParkingSpot spot = parkingSpotRepository.findById(spotId)
                .orElseThrow(() -> new ParkingSpotNotFoundException("No spot found for id " + spotId));
        spot.release();
        parkingSpotRepository.save(spot);
    }
}

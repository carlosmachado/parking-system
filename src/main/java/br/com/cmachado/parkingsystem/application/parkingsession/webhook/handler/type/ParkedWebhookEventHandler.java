package br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventType;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.ParkingSessionNotFoundException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.violations.ParkingSpotNotFoundException;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParkedWebhookEventHandler extends BaseWebhookEventHandler {

    private final ParkingSessionRepository sessionRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    public ParkedWebhookEventHandler(ParkingSessionRepository sessionRepository,
                                     ParkingSpotRepository parkingSpotRepository) {
        this.sessionRepository = sessionRepository;
        this.parkingSpotRepository = parkingSpotRepository;
    }

    @Override
    public WebhookEventType eventType() {
        return WebhookEventType.PARKED;
    }

    @Override
    protected void validate(WebhookEventRequest request) {
        if (request.getLat() == null || request.getLng() == null) {
            throw new WebhookEventValidationException("lat and lng are required for PARKED events");
        }
    }

    @Override
    protected void doHandle(WebhookEventRequest request) {
        var licensePlate = LicensePlate.of(request.getLicensePlate());

        var session = sessionRepository.findByLicensePlateAndStatusIn(licensePlate, List.of(ParkingSessionStatus.ENTERED))
                .orElseThrow(() -> new ParkingSessionNotFoundException("No ENTERED session found for plate " + licensePlate));

        var location = GeoLocation.of(request.getLat(), request.getLng());

        var parkingSpot = parkingSpotRepository.findByLocation(location)
                .orElseThrow(() -> new ParkingSpotNotFoundException("No spot found at location " + location));

        parkingSpot.park(session);
        parkingSpotRepository.save(parkingSpot);
        sessionRepository.save(session);
    }
}

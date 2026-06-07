package br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventType;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.violations.GarageFullException;
import br.com.cmachado.parkingsystem.domain.service.pricing.ChargeCalculator;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EntryWebhookEventHandler extends BaseWebhookEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(EntryWebhookEventHandler.class);

    private final ParkingSessionRepository sessionRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final ChargeCalculator chargeCalculator;
    private final Counter garageFullCounter;

    public EntryWebhookEventHandler(ParkingSessionRepository sessionRepository,
                                    ParkingSpotRepository parkingSpotRepository,
                                    ChargeCalculator chargeCalculator,
                                    MeterRegistry meterRegistry) {
        this.sessionRepository = sessionRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.chargeCalculator = chargeCalculator;
        this.garageFullCounter = Counter.builder("garage.entry.rejected")
                .description("Entry attempts rejected because the garage was at full capacity")
                .register(meterRegistry);
    }

    @Override
    public WebhookEventType eventType() {
        return WebhookEventType.ENTRY;
    }

    @Override
    protected void validate(WebhookEventRequest request) {
        if (request.getEntryTime() == null) {
            throw new WebhookEventValidationException("entry_time is required for ENTRY events");
        }
    }

    @Override
    protected void doHandle(WebhookEventRequest request) {
        var licensePlate = LicensePlate.of(request.getLicensePlate());
        var entryTime = parseTimestamp("entry_time", request.getEntryTime());

        if (hasActiveSession(licensePlate)) {
            logger.info("Duplicate ENTRY ignored: plate={}", licensePlate);
            return;
        }

        if (!hasAvailableSpotInOpenSector(entryTime)) {
            garageFullCounter.increment();
            logger.warn("Entry rejected — garage at capacity or all sectors closed: plate={}", licensePlate);
            throw new GarageFullException(licensePlate);
        }

        var pricing = chargeCalculator.electOnEntry();
        var session = ParkingSession.start(licensePlate, entryTime)
                .charging(pricing.election())
                .strategy(pricing.strategy())
                .build();
        sessionRepository.save(session);

        logger.debug("ENTRY processed: plate={} election={} strategy={}",
                licensePlate, pricing.election(), pricing.strategy());
    }

    private boolean hasAvailableSpotInOpenSector(LocalDateTime entryTime) {
        return parkingSpotRepository.existsAvailableSpotInOpenSector(entryTime.toLocalTime());
    }

    private boolean hasActiveSession(LicensePlate licensePlate) {
        return sessionRepository.findByLicensePlateAndStatusIn(
                licensePlate, List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)).isPresent();
    }
}

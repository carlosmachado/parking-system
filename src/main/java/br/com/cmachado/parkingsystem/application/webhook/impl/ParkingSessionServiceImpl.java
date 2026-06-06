package br.com.cmachado.parkingsystem.application.webhook.impl;

import br.com.cmachado.parkingsystem.application.webhook.ParkingSessionService;
import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.OccupancyRate;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.Period;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.service.occupancy.OccupancyDomainService;
import br.com.cmachado.parkingsystem.domain.service.pricing.PricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.PricingStrategyFactory;
import br.com.cmachado.parkingsystem.infrastructure.http.BadRequestException;
import br.com.cmachado.parkingsystem.infrastructure.http.GarageFullException;
import br.com.cmachado.parkingsystem.infrastructure.http.ParkingSessionNotFoundException;
import br.com.cmachado.parkingsystem.infrastructure.http.ParkingSpotNotFoundException;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Orchestrates the vehicle lifecycle driven by simulator webhook events.
 *
 * <p>Validates and routes each event, then delegates to the matching use-case method.
 * ENTRY is rejected with {@link GarageFullException} (HTTP 409, code EST-001) when the
 * garage is at 100% capacity.</p>
 */
@Service
public class ParkingSessionServiceImpl implements ParkingSessionService {

    private static final Logger logger = LoggerFactory.getLogger(ParkingSessionServiceImpl.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final ParkingSessionRepository sessionRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final SectorRepository sectorRepository;
    private final OccupancyDomainService occupancyDomainService;
    private final PricingStrategyFactory pricingStrategyFactory;
    private final Counter garageFullCounter;

    public ParkingSessionServiceImpl(ParkingSessionRepository sessionRepository,
                                     ParkingSpotRepository parkingSpotRepository,
                                     SectorRepository sectorRepository,
                                     OccupancyDomainService occupancyDomainService,
                                     PricingStrategyFactory pricingStrategyFactory,
                                     MeterRegistry meterRegistry) {
        this.sessionRepository = sessionRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.sectorRepository = sectorRepository;
        this.occupancyDomainService = occupancyDomainService;
        this.pricingStrategyFactory = pricingStrategyFactory;
        this.garageFullCounter = Counter.builder("garage.entry.rejected")
                .description("Entry attempts rejected because the garage was at full capacity")
                .register(meterRegistry);
    }

    @Override
    @Transactional
    public void handle(WebhookEventRequest request) {
        if (request.getLicensePlate() == null || request.getLicensePlate().isBlank())
            throw new BadRequestException("license_plate is required");
        if (request.getEventType() == null || request.getEventType().isBlank())
            throw new BadRequestException("event_type is required");

        switch (request.getEventType().toUpperCase()) {
            case "ENTRY" -> {
                if (request.getEntryTime() == null)
                    throw new BadRequestException("entry_time is required for ENTRY events");
                processEntry(request);
            }
            case "PARKED" -> {
                if (request.getLat() == null || request.getLng() == null)
                    throw new BadRequestException("lat and lng are required for PARKED events");
                processParked(request);
            }
            case "EXIT" -> {
                if (request.getExitTime() == null)
                    throw new BadRequestException("exit_time is required for EXIT events");
                processExit(request);
            }
            default -> throw new BadRequestException("Unknown event_type: " + request.getEventType());
        }
    }

    private void processEntry(WebhookEventRequest request) {
        var licensePlate = LicensePlate.of(request.getLicensePlate());
        var entryTime = LocalDateTime.parse(request.getEntryTime(), FORMATTER);

        if (!parkingSpotRepository.existsByOccupiedFalse()) {
            garageFullCounter.increment();
            logger.warn("Entry rejected — garage at capacity: plate={}", licensePlate);
            throw new GarageFullException(licensePlate);
        }

        sessionRepository.save(ParkingSession.enter(licensePlate, entryTime));
    }

    private void processParked(WebhookEventRequest request) {
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

    private void processExit(WebhookEventRequest request) {
        var licensePlate = LicensePlate.of(request.getLicensePlate());
        var exitTime = LocalDateTime.parse(request.getExitTime(), FORMATTER);

        ParkingSession session = sessionRepository.findByLicensePlateAndStatusIn(
                        licensePlate, List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED))
                .orElseThrow(() -> new ParkingSessionNotFoundException("No active session found for plate " + licensePlate));

        if (session.getStatus() == ParkingSessionStatus.PARKED) {
            ParkingSpot spot = parkingSpotRepository.findById(session.getSpotId())
                    .orElseThrow(() -> new ParkingSpotNotFoundException("No spot found for id " + session.getSpotId()));

            Money amountCharged = calculateCharge(session, spot, exitTime);
            session.exit(exitTime, amountCharged);
            sessionRepository.save(session);

            boolean anotherParkedHere = sessionRepository.existsBySpotIdAndStatusAndIdNot(
                    spot.getId(), ParkingSessionStatus.PARKED, session.getId());
            if (!anotherParkedHere) {
                spot.release();
                parkingSpotRepository.save(spot);
            }
        } else {
            session.exit(exitTime, Money.ZERO);
            sessionRepository.save(session);
        }
    }

    private Money calculateCharge(ParkingSession session, ParkingSpot spot, LocalDateTime exitTime) {
        Sector sector = sectorRepository.findByCode(spot.getSectorCode()).orElse(null);
        if (sector == null) {
            return Money.ZERO;
        }
        int totalSpots = (int) parkingSpotRepository.count();
        int occupiedSpots = (int) parkingSpotRepository.countByOccupiedTrue();
        OccupancyRate occupancyRate = occupancyDomainService.calculateOccupancyRate(totalSpots, occupiedSpots);
        PricingStrategy pricingStrategy = pricingStrategyFactory.getStrategy(occupancyRate);
        Period tempPeriod = session.getPeriod().end(exitTime);
        return pricingStrategy.calculate(tempPeriod, sector.getBasePrice());
    }
}

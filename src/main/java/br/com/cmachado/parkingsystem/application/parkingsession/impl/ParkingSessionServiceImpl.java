package br.com.cmachado.parkingsystem.application.parkingsession.impl;

import br.com.cmachado.parkingsystem.domain.service.pricing.ChargeCalculator;
import br.com.cmachado.parkingsystem.application.parkingsession.ParkingSessionService;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpotId;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.infrastructure.http.BadRequestException;
import br.com.cmachado.parkingsystem.domain.model.spot.GarageFullException;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionNotFoundException;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpotNotFoundException;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ParkingSessionServiceImpl implements ParkingSessionService {

    private static final Logger logger = LoggerFactory.getLogger(ParkingSessionServiceImpl.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final ParkingSessionRepository sessionRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final SectorRepository sectorRepository;
    private final ChargeCalculator chargeCalculator;
    private final Counter garageFullCounter;

    public ParkingSessionServiceImpl(ParkingSessionRepository sessionRepository,
                                     ParkingSpotRepository parkingSpotRepository,
                                     SectorRepository sectorRepository,
                                     ChargeCalculator chargeCalculator,
                                     MeterRegistry meterRegistry) {
        this.sessionRepository = sessionRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.sectorRepository = sectorRepository;
        this.chargeCalculator = chargeCalculator;
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

        if (!hasAvailableSpotInOpenSector()) {
            garageFullCounter.increment();
            logger.warn("Entry rejected — garage at capacity or all sectors closed: plate={}", licensePlate);
            throw new GarageFullException(licensePlate);
        }

        var session = ParkingSession.enter(licensePlate, entryTime);
        sessionRepository.save(session);
    }

    private boolean hasAvailableSpotInOpenSector() {
        var now = LocalTime.now();

        //todo move to query would improve performance, even better if is just one query joining sector and spot
        Set<SectorCode> openCodes = sectorRepository.findAll().stream()
                .filter(s -> s.isOpen(now))
                .map(Sector::getCode)
                .collect(Collectors.toSet());

        return !openCodes.isEmpty() && parkingSpotRepository.existsByOccupiedFalseAndSectorCodeIn(openCodes);
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

        if (session.isParked()) {
            ParkingSpotId spotId = session.getSpotId();
            ParkingSpot spot = parkingSpotRepository.findById(spotId)
                    .orElseThrow(() -> new ParkingSpotNotFoundException("No spot found for id " + spotId));
            spot.release();
            parkingSpotRepository.save(spot);
        }

        chargeCalculator.charge(session, exitTime);
        sessionRepository.save(session);
    }
}

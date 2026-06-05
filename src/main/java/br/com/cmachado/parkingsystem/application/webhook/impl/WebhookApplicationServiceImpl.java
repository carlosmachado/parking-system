package br.com.cmachado.parkingsystem.application.webhook.impl;

import br.com.cmachado.parkingsystem.application.webhook.WebhookApplicationService;
import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.OccupancyRate;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.sector.events.GarageAtCapacity;
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
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates the vehicle lifecycle driven by simulator webhook events.
 *
 * <p>Webhook events are facts — the car already entered, parked, or exited before the
 * HTTP call arrives. This service records and adapts; it never rejects a valid event type.
 * If the garage is at capacity when an entry arrives, a {@link GarageAtCapacity} application
 * event is published for observability.</p>
 */
@Service
public class WebhookApplicationServiceImpl implements WebhookApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookApplicationServiceImpl.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final ParkingSessionRepository sessionRepository;
    private final ParkingSpotRepository spotRepository;
    private final SectorRepository sectorRepository;
    private final OccupancyDomainService occupancyDomainService;
    private final PricingStrategyFactory pricingStrategyFactory;
    private final ApplicationEventPublisher eventPublisher;

    public WebhookApplicationServiceImpl(ParkingSessionRepository sessionRepository,
                                         ParkingSpotRepository spotRepository,
                                         SectorRepository sectorRepository,
                                         OccupancyDomainService occupancyDomainService,
                                         PricingStrategyFactory pricingStrategyFactory,
                                         ApplicationEventPublisher eventPublisher) {
        this.sessionRepository = sessionRepository;
        this.spotRepository = spotRepository;
        this.sectorRepository = sectorRepository;
        this.occupancyDomainService = occupancyDomainService;
        this.pricingStrategyFactory = pricingStrategyFactory;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Records a vehicle entry. Always succeeds — if the garage is at capacity a metric
     * event is published but the entry is still stored.
     */
    @Override
    @Transactional
    public void processEntry(WebhookEventRequest request) {
        LicensePlate licensePlate = new LicensePlate(request.getLicensePlate());
        LocalDateTime entryTime = LocalDateTime.parse(request.getEntryTime(), FORMATTER);

        int totalSpots = (int) spotRepository.count();
        int occupiedSpots = (int) spotRepository.countByOccupiedTrue();
        if (occupancyDomainService.isGarageFull(totalSpots, occupiedSpots)) {
            logger.warn("Entry recorded while garage at capacity: plate={}", licensePlate);
            eventPublisher.publishEvent(new GarageAtCapacity(licensePlate, entryTime));
        }

        sessionRepository.save(ParkingSession.enter(licensePlate, entryTime));
    }

    /**
     * Assigns the nearest available spot. Prefers unoccupied spots; falls back to any
     * spot if none are free (transient overlap allowed). Always sets a spotId on the session.
     */
    @Override
    @Transactional
    public void processParked(WebhookEventRequest request) {
        LicensePlate licensePlate = new LicensePlate(request.getLicensePlate());
        LocalDateTime parkedTime = LocalDateTime.now();

        ParkingSession session = sessionRepository.findByLicensePlateAndStatusIn(
                        licensePlate, List.of(ParkingSessionStatus.ENTERED))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No ENTERED session found for plate " + licensePlate));

        GeoLocation currentLoc = (request.getLat() != null && request.getLng() != null)
                ? new GeoLocation(request.getLat(), request.getLng()) : null;

        ParkingSpot spot = findSpotForParking(currentLoc, licensePlate);
        if (spot == null) {
            logger.warn("No spot found for plate {}; parking session recorded without spot.", licensePlate);
            session.park(null, null, parkedTime);
        } else {
            spot.occupy();
            spotRepository.save(spot);
            session.park(spot.getId(), spot.getSectorCode(), parkedTime);
        }
        sessionRepository.save(session);
    }

    /**
     * Releases the spot and charges the stay. The spot is kept occupied when another
     * session is currently parked on it (transient overlap).
     */
    @Override
    @Transactional
    public void processExit(WebhookEventRequest request) {
        LicensePlate licensePlate = new LicensePlate(request.getLicensePlate());
        LocalDateTime exitTime = LocalDateTime.parse(request.getExitTime(), FORMATTER);

        ParkingSession session = sessionRepository.findByLicensePlateAndStatusIn(
                        licensePlate, List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active session found for plate " + licensePlate));

        Money amountCharged = Money.ZERO;

        if (session.getStatus() == ParkingSessionStatus.PARKED && session.getSpotId() != null) {
            ParkingSpot spot = spotRepository.findById(session.getSpotId()).orElse(null);

            if (spot != null) {
                Sector sector = sectorRepository.findByCode(spot.getSectorCode()).orElse(null);

                if (sector != null) {
                    int totalSpots = (int) spotRepository.count();
                    int occupiedSpots = (int) spotRepository.countByOccupiedTrue();
                    OccupancyRate occupancyRate = occupancyDomainService.calculateOccupancyRate(totalSpots, occupiedSpots);
                    PricingStrategy pricingStrategy = pricingStrategyFactory.getStrategy(occupancyRate);

                    Period tempPeriod = new Period(session.getPeriod().getEntryTime());
                    tempPeriod.setExitTime(exitTime);
                    amountCharged = pricingStrategy.calculate(tempPeriod, sector.getBasePrice());
                }

                session.exit(exitTime, amountCharged);
                sessionRepository.save(session);

                boolean anotherParkedHere = sessionRepository.existsBySpotIdAndStatusAndIdNot(
                        spot.getId(), ParkingSessionStatus.PARKED, session.getId());
                if (!anotherParkedHere) {
                    spot.release();
                    spotRepository.save(spot);
                }
                return;
            }
        }

        session.exit(exitTime, amountCharged);
        sessionRepository.save(session);
    }

    private ParkingSpot findSpotForParking(GeoLocation currentLoc, LicensePlate licensePlate) {
        // Filter to sectors open right now
        LocalTime now = LocalTime.now();
        Set<SectorCode> openSectorCodes = sectorRepository.findAll().stream()
                .filter(s -> s.isOpen(now))
                .map(Sector::getCode)
                .collect(Collectors.toSet());

        if (openSectorCodes.isEmpty()) {
            logger.warn("No open sectors at {}; falling back to all spots for plate {}.", now, licensePlate);
        }

        List<ParkingSpot> candidates = openSectorCodes.isEmpty()
                ? spotRepository.findByOccupiedFalse()
                : spotRepository.findByOccupiedFalseAndSectorCodeIn(openSectorCodes);

        if (candidates.isEmpty()) {
            // Fall back to any spot in open sectors (accept transient overlap)
            candidates = openSectorCodes.isEmpty()
                    ? spotRepository.findAll()
                    : spotRepository.findAll().stream()
                            .filter(s -> openSectorCodes.contains(s.getSectorCode()))
                            .collect(Collectors.toList());
        }

        return occupancyDomainService.findNearestAvailableSpot(candidates, currentLoc).orElse(null);
    }
}

package br.com.cmachado.parkingsystem.application.webhook.impl;

import br.com.cmachado.parkingsystem.application.webhook.WebhookApplicationService;
import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.garage.OccupancyRate;
import br.com.cmachado.parkingsystem.domain.model.garage.Sector;
import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.garage.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.Spot;
import br.com.cmachado.parkingsystem.domain.model.spot.SpotRepository;
import br.com.cmachado.parkingsystem.domain.model.vehicle.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.vehicle.Period;
import br.com.cmachado.parkingsystem.domain.model.vehicle.VehicleEvent;
import br.com.cmachado.parkingsystem.domain.model.vehicle.VehicleEventRepository;
import br.com.cmachado.parkingsystem.domain.model.vehicle.VehicleEventStatus;
import br.com.cmachado.parkingsystem.domain.service.occupancy.OccupancyDomainService;
import br.com.cmachado.parkingsystem.domain.service.pricing.PricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.PricingStrategyFactory;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the vehicle lifecycle driven by simulator webhook events.
 *
 * <p>Each event type performs a state transition on the {@link VehicleEvent} aggregate:
 * ENTRY registers the vehicle (rejected when the garage is full), PARKED assigns the
 * nearest free spot, and EXIT releases the spot and charges the vehicle using the
 * occupancy-based {@link PricingStrategy}.</p>
 */
@Service
public class WebhookApplicationServiceImpl implements WebhookApplicationService {

    private final VehicleEventRepository vehicleEventRepository;
    private final SpotRepository spotRepository;
    private final SectorRepository sectorRepository;
    private final OccupancyDomainService occupancyDomainService;
    private final PricingStrategyFactory pricingStrategyFactory;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    public WebhookApplicationServiceImpl(VehicleEventRepository vehicleEventRepository,
                                         SpotRepository spotRepository,
                                         SectorRepository sectorRepository,
                                         OccupancyDomainService occupancyDomainService,
                                         PricingStrategyFactory pricingStrategyFactory) {
        this.vehicleEventRepository = vehicleEventRepository;
        this.spotRepository = spotRepository;
        this.sectorRepository = sectorRepository;
        this.occupancyDomainService = occupancyDomainService;
        this.pricingStrategyFactory = pricingStrategyFactory;
    }

    /**
     * Registers a vehicle entering the garage. Rejects the entry when the garage is
     * already at full capacity, since no spot can be assigned until one is freed.
     */
    @Override
    public void processEntry(WebhookEventRequest request) {
        LicensePlate licensePlate = new LicensePlate(request.getLicensePlate());
        LocalDateTime entryTime = LocalDateTime.parse(request.getEntryTime(), formatter);

        int totalSpots = (int) spotRepository.count();
        int occupiedSpots = (int) spotRepository.countByOccupiedTrue();
        if (occupancyDomainService.isGarageFull(totalSpots, occupiedSpots)) {
            throw new IllegalStateException("Garage is full, entry not allowed for plate " + licensePlate);
        }

        VehicleEvent event = VehicleEvent.enter(licensePlate, entryTime);
        vehicleEventRepository.save(event);
    }

    /**
     * Assigns the nearest available spot to a previously entered vehicle. The PARKED
     * event carries no timestamp, so the moment the event is processed is used as the
     * parked time.
     */
    @Override
    public void processParked(WebhookEventRequest request) {
        LicensePlate licensePlate = new LicensePlate(request.getLicensePlate());
        LocalDateTime parkedTime = LocalDateTime.now();

        VehicleEvent event = vehicleEventRepository.findByLicensePlateAndStatusIn(
                        licensePlate, List.of(VehicleEventStatus.ENTERED))
                .orElseThrow(() -> new IllegalArgumentException("No ENTERED vehicle found for plate " + licensePlate));

        List<Spot> availableSpots = spotRepository.findByOccupiedFalse();
        GeoLocation currentLoc = (request.getLat() != null && request.getLng() != null) 
                ? new GeoLocation(request.getLat(), request.getLng()) : null;

        Optional<Spot> nearestSpot = occupancyDomainService.findNearestAvailableSpot(availableSpots, currentLoc);
        
        if (nearestSpot.isEmpty()) {
            throw new IllegalStateException("Garage is full, no spots available for plate " + licensePlate);
        }

        Spot spot = nearestSpot.get();
        spot.occupy();
        spotRepository.save(spot);

        event.park(spot.getId(), spot.getSectorCode(), parkedTime);
        vehicleEventRepository.save(event);
    }

    /**
     * Handles a vehicle leaving the garage: releases its spot and, when it was parked,
     * charges the fee derived from the parked duration and current occupancy rate.
     */
    @Override
    public void processExit(WebhookEventRequest request) {
        LicensePlate licensePlate = new LicensePlate(request.getLicensePlate());
        LocalDateTime exitTime = LocalDateTime.parse(request.getExitTime(), formatter);

        VehicleEvent event = vehicleEventRepository.findByLicensePlateAndStatusIn(
                        licensePlate, List.of(VehicleEventStatus.ENTERED, VehicleEventStatus.PARKED))
                .orElseThrow(() -> new IllegalArgumentException("No active vehicle found for plate " + licensePlate));

        Money amountCharged = Money.ZERO;

        if (event.getStatus() == VehicleEventStatus.PARKED) {
            Spot spot = spotRepository.findById(event.getSpotId())
                    .orElseThrow(() -> new IllegalStateException("Spot not found"));

            Sector sector = sectorRepository.findByCode(spot.getSectorCode())
                    .orElseThrow(() -> new IllegalStateException("Sector not found"));

            // Price using the occupancy while the exiting vehicle still holds its spot,
            // then release the spot.
            int totalSpots = (int) spotRepository.count();
            int occupiedSpots = (int) spotRepository.countByOccupiedTrue();
            OccupancyRate occupancyRate = occupancyDomainService.calculateOccupancyRate(totalSpots, occupiedSpots);

            PricingStrategy pricingStrategy = pricingStrategyFactory.getStrategy(occupancyRate);

            // Compute the fee from the full stay (entry to exit) before finalizing the event.
            Period tempPeriod = new Period(event.getPeriod().getEntryTime());
            tempPeriod.setExitTime(exitTime);
            amountCharged = pricingStrategy.calculate(tempPeriod, sector.getBasePrice());

            spot.release();
            spotRepository.save(spot);
        }

        event.exit(exitTime, amountCharged);
        vehicleEventRepository.save(event);
    }
}

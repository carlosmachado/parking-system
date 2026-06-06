package br.com.cmachado.parkingsystem.application.garage.impl;

import br.com.cmachado.parkingsystem.application.garage.GarageInitializerService;
import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.infrastructure.client.GarageResponse;
import br.com.cmachado.parkingsystem.infrastructure.client.GarageResponse.SectorData;
import br.com.cmachado.parkingsystem.infrastructure.client.GarageResponse.SpotData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

/**
 * Syncs the garage layout from the simulator. Both sectors and spots are upserted so
 * re-running initialization on restart always reflects the latest simulator data.
 */
@Service
public class GarageInitializerServiceImpl implements GarageInitializerService {

    private static final Logger logger = LoggerFactory.getLogger(GarageInitializerServiceImpl.class);

    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository spotRepository;

    public GarageInitializerServiceImpl(SectorRepository sectorRepository,
                                        ParkingSpotRepository spotRepository) {
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
    }

    @Override
    @Transactional
    public void initializeGarage(GarageResponse config) {
        if (config == null) {
            logger.warn("Received null garage config from simulator.");
            return;
        }

        initializeSectors(config.getGarage());
        initializeSpots(config.getSpots());

        logger.info("Garage synced: {} sectors, {} spots.",
                config.getGarage() != null ? config.getGarage().size() : 0,
                config.getSpots() != null ? config.getSpots().size() : 0);
    }

    private void initializeSectors(List<SectorData> sectors) {
        if (sectors == null) return;
        sectors.forEach(this::upsertSector);
        logger.info("Synced {} sectors.", sectors.size());
    }

    private void upsertSector(SectorData data) {
        var code = SectorCode.of(data.getSector());
        var basePrice = Money.of(data.getBasePrice());
        LocalTime openHour = parseTime(data.getOpenHour(), LocalTime.MIDNIGHT);
        LocalTime closeHour = parseTime(data.getCloseHour(), LocalTime.of(23, 59));
        Integer durationLimitMinutes = data.getDurationLimitMinutes() != null ? data.getDurationLimitMinutes() : 1440;

        Sector sector = sectorRepository.findByCode(code)
                .orElseGet(() -> Sector.register(code, basePrice, data.getMaxCapacity(),
                        openHour, closeHour, durationLimitMinutes));

        sector.update(basePrice, data.getMaxCapacity(), openHour, closeHour, durationLimitMinutes);

        sectorRepository.save(sector);

        logger.info("Synced sector {} (price={}, capacity={}, hours={}-{}).",
                data.getSector(), data.getBasePrice(), data.getMaxCapacity(), openHour, closeHour);
    }

    private void initializeSpots(List<SpotData> spots) {
        if (spots == null) return;
        spots.forEach(this::upsertSpot);
        logger.info("Synced {} spots.", spots.size());
    }

    private void upsertSpot(SpotData data) {
        var code = SectorCode.of(data.getSector());

        var location = GeoLocation.of(data.getLat(), data.getLng());

        ParkingSpot spot = spotRepository.findByExternalId(data.getId())
                .orElseGet(() -> ParkingSpot.register(data.getId(), code, location));

        spot.updateLocation(code, location);

        spotRepository.save(spot);
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return LocalTime.parse(value);
        } catch (Exception e) {
            logger.warn("Could not parse time '{}', using default {}.", value, fallback);
            return fallback;
        }
    }
}

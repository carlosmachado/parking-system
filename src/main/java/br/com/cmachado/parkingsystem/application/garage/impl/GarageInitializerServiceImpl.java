package br.com.cmachado.parkingsystem.application.garage.impl;

import br.com.cmachado.parkingsystem.application.garage.GarageInitializerService;
import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.garage.Sector;
import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.garage.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.Spot;
import br.com.cmachado.parkingsystem.domain.model.spot.SpotRepository;
import br.com.cmachado.parkingsystem.infrastructure.client.GarageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the garage layout fetched from the simulator. Sectors and spots are created
 * idempotently, so re-running initialization does not duplicate existing records.
 */
@Service
public class GarageInitializerServiceImpl implements GarageInitializerService {

    private static final Logger logger = LoggerFactory.getLogger(GarageInitializerServiceImpl.class);

    private final SectorRepository sectorRepository;
    private final SpotRepository spotRepository;

    public GarageInitializerServiceImpl(SectorRepository sectorRepository, SpotRepository spotRepository) {
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
    }

    @Override
    @Transactional
    public void initializeGarage(GarageResponse config) {
        logger.info("Initializing garage from simulator config...");

        if (config == null) {
            logger.warn("Received null garage config from simulator.");
            return;
        }

        // Persist sectors
        if (config.getGarage() != null) {
            for (GarageResponse.SectorData sectorData : config.getGarage()) {
                SectorCode code = new SectorCode(sectorData.getSector());
                Money basePrice = Money.of(sectorData.getBasePrice());
                Integer maxCapacity = sectorData.getMaxCapacity();

                sectorRepository.findByCode(code)
                        .orElseGet(() -> {
                            Sector sector = new Sector(code, basePrice, maxCapacity);
                            return sectorRepository.save(sector);
                        });
                logger.info("Configured Sector: {} with base_price={} max_capacity={}",
                        sectorData.getSector(), sectorData.getBasePrice(), maxCapacity);
            }
        }

        // Persist spots
        if (config.getSpots() != null) {
            for (GarageResponse.SpotData spotData : config.getSpots()) {
                SectorCode code = new SectorCode(spotData.getSector());
                GeoLocation location = new GeoLocation(spotData.getLat(), spotData.getLng());

                spotRepository.findById(spotData.getId())
                        .orElseGet(() -> {
                            Spot spot = new Spot(spotData.getId(), code, location);
                            return spotRepository.save(spot);
                        });
            }
            logger.info("Configured {} spots.", config.getSpots().size());
        }

        logger.info("Garage initialization completed: {} sectors, {} spots.",
                config.getGarage() != null ? config.getGarage().size() : 0,
                config.getSpots() != null ? config.getSpots().size() : 0);
    }
}

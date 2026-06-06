package br.com.cmachado.parkingsystem.domain.service.pricing;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.Period;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.sector.OccupancyRate;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

@Component
public class ChargeCalculator {

    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final PricingStrategyFactory pricingStrategyFactory;

    public ChargeCalculator(SectorRepository sectorRepository,
                            ParkingSpotRepository parkingSpotRepository,
                            PricingStrategyFactory pricingStrategyFactory) {
        this.sectorRepository = sectorRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.pricingStrategyFactory = pricingStrategyFactory;
    }

    public void charge(ParkingSession session, LocalDateTime exitTime) {
        Money basePrice = resolveBasePrice(session.getSectorCode());

        double rawRate = Optional.ofNullable(parkingSpotRepository.findOccupancyRate()).orElse(0.0);

        OccupancyRate occupancyRate = OccupancyRate.of(Math.min(1.0, rawRate), LocalDateTime.now());

        PricingStrategy strategy = pricingStrategyFactory.getStrategy(occupancyRate);

        Period period = session.getPeriod().end(exitTime);

        var amountCharged = strategy.calculate(period, basePrice);
        
        session.exit(exitTime, amountCharged);
    }

    private Money resolveBasePrice(SectorCode sectorCode) {
        if (sectorCode != null) {
            Optional<Sector> sector = sectorRepository.findByCode(sectorCode);
            if (sector.isPresent()) return sector.get().getBasePrice();
        }

        return sectorRepository.findAll().stream()
                .map(Sector::getBasePrice)
                .min(Comparator.comparing(Money::getAmount))
                .orElse(Money.ZERO);
    }
}

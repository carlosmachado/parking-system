package br.com.cmachado.parkingsystem.domain.service.pricing;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.Period;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.sector.OccupancyRate;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyFactory;
import br.com.cmachado.parkingsystem.domain.shared.DomainService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@DomainService
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

        PricingStrategy strategy = pricingStrategyFactory.getStrategy(resolveOccupancyRate());

        Period period = session.getPeriod().end(exitTime);

        var amountCharged = strategy.calculate(period, basePrice);

        session.exit(exitTime, amountCharged);
    }

    private OccupancyRate resolveOccupancyRate() {
        double rawRate = Optional.ofNullable(parkingSpotRepository.findOccupancyRate()).orElse(0.0);
        double clampedRate = Math.clamp(rawRate, 0.0, 1.0);
        return OccupancyRate.of(clampedRate, LocalDateTime.now());
    }

    private Money resolveBasePrice(SectorCode sectorCode) {
        if (sectorCode != null) {
            Optional<Sector> sector = sectorRepository.findByCode(sectorCode);
            if (sector.isPresent()) return sector.get().getBasePrice();
        }

        return sectorRepository.findMinBasePrice().map(Money::of).orElse(Money.ZERO);
    }
}

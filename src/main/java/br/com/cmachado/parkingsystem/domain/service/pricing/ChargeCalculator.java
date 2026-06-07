package br.com.cmachado.parkingsystem.domain.service.pricing;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.Period;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.PricingElection;
import br.com.cmachado.parkingsystem.domain.model.sector.OccupancyRate;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyFactory;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyType;
import br.com.cmachado.parkingsystem.domain.shared.DomainService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@DomainService
public class ChargeCalculator {

    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final PricingStrategyFactory pricingStrategyFactory;
    private final PricingElection election;

    public ChargeCalculator(SectorRepository sectorRepository,
                            ParkingSpotRepository parkingSpotRepository,
                            PricingStrategyFactory pricingStrategyFactory,
                            @Value("${app.pricing.election:AT_EXIT}") PricingElection election) {
        this.sectorRepository = sectorRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.pricingStrategyFactory = pricingStrategyFactory;
        this.election = election;
    }

    /** Decides the pricing to record when a session is created. */
    public EntryPricing electOnEntry() {
        if (shouldElectPricingAtExit())
            return new EntryPricing(PricingElection.AT_EXIT, null);

        var type = pricingStrategyFactory.electType(resolveOccupancyRate());
        return new EntryPricing(PricingElection.AT_ENTRY, type);
    }

    private boolean shouldElectPricingAtExit() {
        return election == PricingElection.AT_EXIT;
    }

    /**
     * Charges the session for its stay and exits it. Uses the strategy elected at entry when
     * present (no occupancy lookup); otherwise elects one from the current occupancy.
     */
    public void charge(ParkingSession session, LocalDateTime exitTime) {
        PricingStrategyType type = extractFromSessionOrElectPricing(session);

        Money basePrice = resolveBasePrice(session.getSectorCode());

        PricingStrategy strategy = pricingStrategyFactory.getStrategy(type);

        Period period = session.getPeriod().end(exitTime);

        Money amountCharged = strategy.calculate(period, basePrice);

        session.exit(exitTime, amountCharged, type);
    }

    private PricingStrategyType extractFromSessionOrElectPricing(ParkingSession session) {
        return session.getPricingElection() == PricingElection.AT_ENTRY
                ? session.getPricingStrategy()
                : pricingStrategyFactory.electType(resolveOccupancyRate());
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

    /**
     * The pricing to stamp on a session at entry: always the configured {@link PricingElection},
     * plus the elected {@link PricingStrategyType} when electing {@code AT_ENTRY} (otherwise null,
     * deferring the choice to exit).
     */
    public record EntryPricing(PricingElection election, PricingStrategyType strategy) { }
}

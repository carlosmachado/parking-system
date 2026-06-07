package br.com.cmachado.parkingsystem.domain.service.pricing.strategy;

import br.com.cmachado.parkingsystem.domain.model.sector.OccupancyRate;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.type.DiscountPricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.type.StandardPricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.type.Surcharge10PricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.type.Surcharge25PricingStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Selects the dynamic pricing strategy from the occupancy rate:
 * {@code <25%} → 10% discount, {@code <50%} → base price, {@code <75%} → 10% surcharge,
 * otherwise 25% surcharge. The elected {@link PricingStrategyType} can be persisted and the
 * matching strategy looked back up via {@link #getStrategy(PricingStrategyType)}.
 */
@Component
public class PricingStrategyFactory {

    private final Map<PricingStrategyType, PricingStrategy> strategiesByType;

    public PricingStrategyFactory(
            DiscountPricingStrategy discountStrategy,
            StandardPricingStrategy standardStrategy,
            Surcharge10PricingStrategy surcharge10Strategy,
            Surcharge25PricingStrategy surcharge25Strategy) {
        this.strategiesByType = new EnumMap<>(PricingStrategyType.class);
        register(discountStrategy);
        register(standardStrategy);
        register(surcharge10Strategy);
        register(surcharge25Strategy);
    }

    private void register(PricingStrategy strategy) {
        this.strategiesByType.put(strategy.getType(), strategy);
    }

    /** Elects the strategy type for the given occupancy rate. */
    public PricingStrategyType electType(OccupancyRate occupancyRate) {
        double rate = occupancyRate.getRate();
        if (rate < 0.25) return PricingStrategyType.DISCOUNT;
        if (rate < 0.50) return PricingStrategyType.STANDARD;
        if (rate < 0.75) return PricingStrategyType.SURCHARGE_10;
        return PricingStrategyType.SURCHARGE_25;
    }

    public PricingStrategy getStrategy(PricingStrategyType type) {
        PricingStrategy strategy = strategiesByType.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No pricing strategy registered for type '%s'".formatted(type));
        }
        return strategy;
    }

    public PricingStrategy getStrategy(OccupancyRate occupancyRate) {
        return getStrategy(electType(occupancyRate));
    }
}

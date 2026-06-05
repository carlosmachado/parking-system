package br.com.cmachado.parkingsystem.domain.service.pricing;

import br.com.cmachado.parkingsystem.domain.model.sector.OccupancyRate;
import org.springframework.stereotype.Component;

/**
 * Selects the dynamic pricing strategy from the current occupancy rate:
 * {@code <25%} → 10% discount, {@code <50%} → base price, {@code <75%} → 10% surcharge,
 * otherwise 25% surcharge.
 */
@Component
public class PricingStrategyFactory {

    private final DiscountPricingStrategy discountStrategy;
    private final StandardPricingStrategy standardStrategy;
    private final Surcharge10PricingStrategy surcharge10Strategy;
    private final Surcharge25PricingStrategy surcharge25Strategy;

    public PricingStrategyFactory(
            DiscountPricingStrategy discountStrategy,
            StandardPricingStrategy standardStrategy,
            Surcharge10PricingStrategy surcharge10Strategy,
            Surcharge25PricingStrategy surcharge25Strategy) {
        this.discountStrategy = discountStrategy;
        this.standardStrategy = standardStrategy;
        this.surcharge10Strategy = surcharge10Strategy;
        this.surcharge25Strategy = surcharge25Strategy;
    }

    public PricingStrategy getStrategy(OccupancyRate occupancyRate) {
        double rate = occupancyRate.getRate();
        if (rate < 0.25) return discountStrategy;
        if (rate < 0.50) return standardStrategy;
        if (rate < 0.75) return surcharge10Strategy;
        return surcharge25Strategy;
    }
}

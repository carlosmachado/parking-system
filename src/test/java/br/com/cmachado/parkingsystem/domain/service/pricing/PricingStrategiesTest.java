package br.com.cmachado.parkingsystem.domain.service.pricing;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.OccupancyRate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.Period;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.*;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.type.DiscountPricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.type.StandardPricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.type.Surcharge10PricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.type.Surcharge25PricingStrategy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PricingStrategiesTest {

    private final DiscountPricingStrategy discountStrategy = new DiscountPricingStrategy();
    private final StandardPricingStrategy standardStrategy = new StandardPricingStrategy();
    private final Surcharge10PricingStrategy surcharge10Strategy = new Surcharge10PricingStrategy();
    private final Surcharge25PricingStrategy surcharge25Strategy = new Surcharge25PricingStrategy();

    private final PricingStrategyFactory factory = new PricingStrategyFactory(
            discountStrategy, standardStrategy, surcharge10Strategy, surcharge25Strategy);

    private static final Money BASE = Money.of(10.00);

    @Test
    void firstThirtyMinutesAreFree() {
        // arrange
        Period period = periodOfMinutes(20);

        // act / assert
        assertEquals(Money.ZERO, standardStrategy.calculate(period, BASE), "stays under 30 min are free");
    }

    @Test
    void standardChargesBasePricePerStartedHour() {
        // arrange
        Period period = periodOfMinutes(60);

        // act / assert
        assertEquals(Money.of(10.00), standardStrategy.calculate(period, BASE), "1h at base price");
    }

    @Test
    void fractionOfAnHourRoundsUp() {
        // arrange
        Period period = periodOfMinutes(65);

        // act / assert
        assertEquals(Money.of(20.00), standardStrategy.calculate(period, BASE), "65 min bills as 2 hours");
    }

    @Test
    void discountAppliesTenPercentOff() {
        // arrange
        Period period = periodOfMinutes(60);

        // act / assert
        assertEquals(Money.of(9.00), discountStrategy.calculate(period, BASE), "1h with 10% discount");
    }

    @Test
    void surcharge10AppliesTenPercentExtra() {
        // arrange
        Period period = periodOfMinutes(60);

        // act / assert
        assertEquals(Money.of(11.00), surcharge10Strategy.calculate(period, BASE), "1h with 10% surcharge");
    }

    @Test
    void surcharge25AppliesTwentyFivePercentExtra() {
        // arrange
        Period period = periodOfMinutes(60);

        // act / assert
        assertEquals(Money.of(12.50), surcharge25Strategy.calculate(period, BASE), "1h with 25% surcharge");
    }

    @Test
    void eachStrategyReportsItsType() {
        // act / assert
        assertEquals(PricingStrategyType.DISCOUNT, discountStrategy.getType());
        assertEquals(PricingStrategyType.STANDARD, standardStrategy.getType());
        assertEquals(PricingStrategyType.SURCHARGE_10, surcharge10Strategy.getType());
        assertEquals(PricingStrategyType.SURCHARGE_25, surcharge25Strategy.getType());
    }

    @Test
    void factorySelectsStrategyByOccupancyTier() {
        // act / assert
        assertEquals(discountStrategy, factory.getStrategy(occupancy(0.10)), "< 25% → discount");
        assertEquals(standardStrategy, factory.getStrategy(occupancy(0.30)), "< 50% → standard");
        assertEquals(surcharge10Strategy, factory.getStrategy(occupancy(0.60)), "< 75% → 10% surcharge");
        assertEquals(surcharge25Strategy, factory.getStrategy(occupancy(0.80)), "≥ 75% → 25% surcharge");
        assertEquals(surcharge25Strategy, factory.getStrategy(occupancy(1.00)), "100% → 25% surcharge");
    }

    @Test
    void factoryElectsTypeByOccupancyTier() {
        // act / assert
        assertEquals(PricingStrategyType.DISCOUNT, factory.electType(occupancy(0.10)), "< 25% → DISCOUNT");
        assertEquals(PricingStrategyType.STANDARD, factory.electType(occupancy(0.30)), "< 50% → STANDARD");
        assertEquals(PricingStrategyType.SURCHARGE_10, factory.electType(occupancy(0.60)), "< 75% → SURCHARGE_10");
        assertEquals(PricingStrategyType.SURCHARGE_25, factory.electType(occupancy(0.80)), "≥ 75% → SURCHARGE_25");
    }

    @Test
    void factoryGetStrategyByTypeReturnsSameInstance() {
        // act / assert
        assertSame(discountStrategy, factory.getStrategy(PricingStrategyType.DISCOUNT));
        assertSame(standardStrategy, factory.getStrategy(PricingStrategyType.STANDARD));
        assertSame(surcharge10Strategy, factory.getStrategy(PricingStrategyType.SURCHARGE_10));
        assertSame(surcharge25Strategy, factory.getStrategy(PricingStrategyType.SURCHARGE_25));
    }

    private Period periodOfMinutes(long minutes) {
        LocalDateTime now = LocalDateTime.now();
        return Period.start(now.minusMinutes(minutes)).end(now);
    }

    private OccupancyRate occupancy(double rate) {
        return OccupancyRate.of(rate, LocalDateTime.now());
    }
}

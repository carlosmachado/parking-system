package br.com.cmachado.parkingsystem.domain.service.pricing;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.garage.OccupancyRate;
import br.com.cmachado.parkingsystem.domain.model.vehicle.Period;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingStrategiesTest {

    private final DiscountPricingStrategy discountStrategy = new DiscountPricingStrategy();
    private final StandardPricingStrategy standardStrategy = new StandardPricingStrategy();
    private final Surcharge10PricingStrategy surcharge10Strategy = new Surcharge10PricingStrategy();
    private final Surcharge25PricingStrategy surcharge25Strategy = new Surcharge25PricingStrategy();

    private final PricingStrategyFactory factory = new PricingStrategyFactory(
            discountStrategy, standardStrategy, surcharge10Strategy, surcharge25Strategy);

    @Test
    void testFreeFirst30Minutes() {
        Period period = new Period(LocalDateTime.now().minusMinutes(20));
        period.setExitTime(LocalDateTime.now());
        
        Money basePrice = Money.of(10.00);

        assertEquals(Money.ZERO, standardStrategy.calculate(period, basePrice));
    }

    @Test
    void testStandardPricing1Hour() {
        Period period = new Period(LocalDateTime.now().minusMinutes(60));
        period.setExitTime(LocalDateTime.now());
        
        Money basePrice = Money.of(10.00);

        assertEquals(Money.of(10.00), standardStrategy.calculate(period, basePrice));
    }

    @Test
    void testStandardPricingFractionOfHourRoundsUp() {
        Period period = new Period(LocalDateTime.now().minusMinutes(65));
        period.setExitTime(LocalDateTime.now());
        
        Money basePrice = Money.of(10.00);

        // 65 minutes = 2 hours
        assertEquals(Money.of(20.00), standardStrategy.calculate(period, basePrice));
    }

    @Test
    void testDiscountStrategy() {
        Period period = new Period(LocalDateTime.now().minusMinutes(60));
        period.setExitTime(LocalDateTime.now());
        
        Money basePrice = Money.of(10.00);

        assertEquals(Money.of(9.00), discountStrategy.calculate(period, basePrice));
    }

    @Test
    void testSurcharge10Strategy() {
        Period period = new Period(LocalDateTime.now().minusMinutes(60));
        period.setExitTime(LocalDateTime.now());
        
        Money basePrice = Money.of(10.00);

        assertEquals(Money.of(11.00), surcharge10Strategy.calculate(period, basePrice));
    }

    @Test
    void testSurcharge25Strategy() {
        Period period = new Period(LocalDateTime.now().minusMinutes(60));
        period.setExitTime(LocalDateTime.now());
        
        Money basePrice = Money.of(10.00);

        assertEquals(Money.of(12.50), surcharge25Strategy.calculate(period, basePrice));
    }

    @Test
    void testFactoryReturnsCorrectStrategy() {
        assertEquals(discountStrategy, factory.getStrategy(new OccupancyRate(0.10, LocalDateTime.now())));
        assertEquals(standardStrategy, factory.getStrategy(new OccupancyRate(0.30, LocalDateTime.now())));
        assertEquals(surcharge10Strategy, factory.getStrategy(new OccupancyRate(0.60, LocalDateTime.now())));
        assertEquals(surcharge25Strategy, factory.getStrategy(new OccupancyRate(0.80, LocalDateTime.now())));
        assertEquals(surcharge25Strategy, factory.getStrategy(new OccupancyRate(1.00, LocalDateTime.now())));
    }
}

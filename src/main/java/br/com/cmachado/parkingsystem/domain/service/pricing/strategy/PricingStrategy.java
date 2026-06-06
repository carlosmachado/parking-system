package br.com.cmachado.parkingsystem.domain.service.pricing.strategy;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.Period;

/**
 * Strategy for computing the parking fee for a stay, selected by occupancy rate.
 */
public interface PricingStrategy {

    /**
     * @return the amount to charge for {@code period} given the sector's {@code basePrice}
     */
    Money calculate(Period period, Money basePrice);
}

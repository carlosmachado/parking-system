package br.com.cmachado.parkingsystem.domain.service.pricing;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.Period;

/**
 * Shared fee calculation: the first 30 minutes are free; beyond that the stay is billed
 * per started hour (rounded up, first hour included) and then adjusted by the subclass's
 * occupancy multiplier via {@link #applyMultiplier(Money)}.
 */
public abstract class BasePricingStrategy implements PricingStrategy {

    @Override
    public Money calculate(Period period, Money basePrice) {
        long minutes = period.getDurationInMinutes();
        if (minutes <= 30) {
            return Money.ZERO;
        }

        long hours = (long) Math.ceil(minutes / 60.0);
        Money baseAmount = basePrice.multiply(hours);

        return applyMultiplier(baseAmount);
    }

    protected abstract Money applyMultiplier(Money baseAmount);
}

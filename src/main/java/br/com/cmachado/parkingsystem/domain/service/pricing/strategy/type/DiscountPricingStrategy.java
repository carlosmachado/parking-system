package br.com.cmachado.parkingsystem.domain.service.pricing.strategy.type;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyType;
import org.springframework.stereotype.Component;

@Component
public class DiscountPricingStrategy extends BasePricingStrategy {
    @Override
    protected Money applyMultiplier(Money baseAmount) {
        return baseAmount.multiply(0.90);
    }

    @Override
    public PricingStrategyType getType() {
        return PricingStrategyType.DISCOUNT;
    }
}

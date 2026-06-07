package br.com.cmachado.parkingsystem.domain.service.pricing.strategy.type;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyType;
import org.springframework.stereotype.Component;

@Component
public class Surcharge10PricingStrategy extends BasePricingStrategy {
    @Override
    protected Money applyMultiplier(Money baseAmount) {
        return baseAmount.multiply(1.10);
    }

    @Override
    public PricingStrategyType getType() {
        return PricingStrategyType.SURCHARGE_10;
    }
}

package br.com.cmachado.parkingsystem.domain.service.pricing.strategy;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import org.springframework.stereotype.Component;

@Component
public class DiscountPricingStrategy extends BasePricingStrategy {
    @Override
    protected Money applyMultiplier(Money baseAmount) {
        return baseAmount.multiply(0.90);
    }
}

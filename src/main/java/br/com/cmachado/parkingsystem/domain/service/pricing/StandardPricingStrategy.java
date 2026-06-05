package br.com.cmachado.parkingsystem.domain.service.pricing;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import org.springframework.stereotype.Component;

@Component
public class StandardPricingStrategy extends BasePricingStrategy {
    @Override
    protected Money applyMultiplier(Money baseAmount) {
        return baseAmount; // No multiplier
    }
}

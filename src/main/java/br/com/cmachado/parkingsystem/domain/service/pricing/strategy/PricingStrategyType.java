package br.com.cmachado.parkingsystem.domain.service.pricing.strategy;

/**
 * Identifies a {@link PricingStrategy} so the elected strategy can be persisted on a session and
 * reloaded later without recomputing occupancy.
 */
public enum PricingStrategyType {
    DISCOUNT,
    STANDARD,
    SURCHARGE_10,
    SURCHARGE_25
}

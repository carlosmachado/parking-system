package br.com.cmachado.parkingsystem.domain.model.parkingsession;

/**
 * When the pricing strategy for a session is elected: {@code AT_ENTRY} locks it in from the
 * occupancy at entry; {@code AT_EXIT} (default) chooses it from the occupancy at exit.
 */
public enum PricingElection {
    AT_ENTRY,
    AT_EXIT
}

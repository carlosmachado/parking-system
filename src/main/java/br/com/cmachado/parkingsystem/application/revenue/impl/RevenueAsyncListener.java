package br.com.cmachado.parkingsystem.application.revenue.impl;

import br.com.cmachado.parkingsystem.domain.model.vehicle.events.VehicleExited;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Updates the daily revenue when a vehicle exits. Runs asynchronously after the exit
 * transaction commits, so revenue bookkeeping never blocks or rolls back the webhook request.
 *
 * <p>The actual database write lives in {@link RevenueUpdater}, which takes the daily-revenue
 * row under a pessimistic lock so concurrent exits serialize without losing increments. The
 * only race left is the very first exit of the day, where two transactions can both try to
 * insert the row; the loser hits the unique constraint and this retry loop re-runs it, by
 * which point the row exists and the locked path applies.</p>
 */
@Component
public class RevenueAsyncListener {

    /** Attempts for the cold-start insert race on a sector's first exit of the day. */
    private static final int MAX_ATTEMPTS = 3;

    private final RevenueUpdater revenueUpdater;

    public RevenueAsyncListener(RevenueUpdater revenueUpdater) {
        this.revenueUpdater = revenueUpdater;
    }

    /**
     * Adds the charged amount to the sector's running total for the exit date. Zero/empty
     * charges are skipped. Retries on optimistic-lock conflict so concurrent exits don't lose
     * updates.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVehicleExited(VehicleExited event) {
        if (event.getAmountCharged() == null || event.getAmountCharged().getAmount().signum() == 0) {
            return; // No revenue to add
        }

        for (int attempt = 1; ; attempt++) {
            try {
                revenueUpdater.addRevenue(event.getSectorCode(), event.getExitDate(), event.getAmountCharged());
                return;
            } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException ex) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw ex;
                }
            }
        }
    }
}

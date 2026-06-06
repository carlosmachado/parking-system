package br.com.cmachado.parkingsystem.application.revenue.daily;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleExited;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>The actual database write lives in {@link DailyRevenueUpdater}, which takes the daily-revenue
 * row under a pessimistic lock so concurrent exits serialize without losing increments. The
 * only race left is the very first exit of the day, where two transactions can both try to
 * insert the row; the loser hits the unique constraint and this retry loop re-runs it, by
 * which point the row exists and the locked path applies.</p>
 *
 * <p>Because there is no outbox or event queue, this listener must never propagate a failure:
 * a thrown exception from an {@code @Async} method is only logged by Spring's uncaught handler,
 * and the revenue increment would be permanently lost. After exhausting retries the failure is
 * logged as an ERROR and a Micrometer counter is incremented, but execution always returns
 * normally.</p>
 */
@Component
public class DailyRevenueAsyncListener {

    private static final Logger logger = LoggerFactory.getLogger(DailyRevenueAsyncListener.class);

    /**
     * Attempts for the cold-start insert race on a sector's first exit of the day.
     */
    private static final int MAX_ATTEMPTS = 5;

    private final DailyRevenueUpdater revenueUpdater;
    private final Counter revenueFailedCounter;

    public DailyRevenueAsyncListener(DailyRevenueUpdater revenueUpdater, MeterRegistry meterRegistry) {
        this.revenueUpdater = revenueUpdater;
        this.revenueFailedCounter = Counter.builder("revenue.update.failed")
                .description("Revenue increments permanently lost due to persistent DB failure")
                .register(meterRegistry);
    }

    /**
     * Adds the charged amount to the sector's running total for the exit date. Zero/empty
     * charges are skipped. Retries with exponential backoff on transient DB conflicts so
     * concurrent exits don't lose updates. Never propagates an exception.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVehicleExited(VehicleExited event) {
        var session = event.getSession();
        if (session.hasNoCharge()) {
            return;
        }

        var sectorCode = session.getSectorCode();
        var exitDate = session.getPeriod().getExitTime().toLocalDate();
        var amountCharged = session.getAmountCharged();

        try {
            for (int attempt = 1; ; attempt++) {
                try {

                    revenueUpdater.addRevenue(sectorCode, exitDate, amountCharged);
                    return;
                } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException ex) {
                    if (attempt >= MAX_ATTEMPTS) {
                        throw ex;
                    }
                    Thread.sleep(50L * attempt);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            revenueFailedCounter.increment();
            logger.error("Revenue update interrupted — increment lost: sector={} date={} amount={}",
                    sectorCode, exitDate, amountCharged, ex);
        } catch (Exception ex) {
            revenueFailedCounter.increment();
            logger.error("Revenue update failed after {} attempts — increment lost: sector={} date={} amount={}",
                    MAX_ATTEMPTS, sectorCode, exitDate, amountCharged, ex);
        }
    }
}

package br.com.cmachado.parkingsystem.application.revenue.daily;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleExited;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Updates the daily revenue when a vehicle exits. Runs asynchronously after the exit
 * transaction commits, so revenue bookkeeping never blocks or rolls back the webhook request.
 *
 * <p>The actual database write lives in {@link DailyRevenueUpdater#addRevenue}, a single atomic
 * upsert ({@code INSERT … ON DUPLICATE KEY UPDATE}). InnoDB serializes concurrent upserts on the
 * {@code (sector_code, date)} row, so no increment is lost and no retry loop is needed.</p>
 *
 * <p>Because there is no outbox or event queue, this listener must never propagate a failure:
 * a thrown exception from an {@code @Async} method is only logged by Spring's uncaught handler,
 * and the revenue increment would be lost. Any unexpected DB error is logged as an ERROR and a
 * Micrometer counter is incremented, but execution always returns normally.</p>
 */
@Component
public class DailyRevenueAsyncListener {

    private static final Logger logger = LoggerFactory.getLogger(DailyRevenueAsyncListener.class);

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
     * charges are skipped. Never propagates an exception.
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
            revenueUpdater.addRevenue(sectorCode, exitDate, amountCharged);
        } catch (Exception ex) {
            revenueFailedCounter.increment();
            logger.error("Revenue update failed — increment lost: sector={} date={} amount={}",
                    sectorCode, exitDate, amountCharged, ex);
        }
    }
}

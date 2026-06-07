package br.com.cmachado.parkingsystem.application.revenue.daily;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleExited;
import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

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
    private final int maxAttempts;
    private final long retryDelayMs;

    public DailyRevenueAsyncListener(DailyRevenueUpdater revenueUpdater,
                                     MeterRegistry meterRegistry,
                                     @Value("${revenue.update.max-attempts:3}") int maxAttempts,
                                     @Value("${revenue.update.retry-delay-ms:100}") long retryDelayMs) {
        this.revenueUpdater = revenueUpdater;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryDelayMs = Math.max(0, retryDelayMs);
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
            logger.debug("Revenue update skipped — no charge: plate={}", session.getLicensePlate());
            return;
        }

        var sectorCode = session.getSectorCode();
        var exitDate = session.getPeriod().getExitTime().toLocalDate();
        var amountCharged = session.getAmountCharged();

        addRevenueWithRetry(sectorCode, exitDate, amountCharged);
    }

    private void addRevenueWithRetry(SectorCode sectorCode, LocalDate exitDate, Money amountCharged) {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                revenueUpdater.addRevenue(sectorCode, exitDate, amountCharged);
                logger.debug("Revenue updated: sector={} date={} amount={}", sectorCode, exitDate, amountCharged);
                return;
            } catch (Exception ex) {
                lastFailure = ex;
                if (attempt < maxAttempts) {
                    logger.warn("Revenue update failed; retrying attempt {}/{}: sector={} date={} amount={}",
                            attempt + 1, maxAttempts, sectorCode, exitDate, amountCharged, ex);
                    sleepBeforeRetry();
                }
            }
        }

        revenueFailedCounter.increment();
        logger.error("Revenue update failed — increment lost: sector={} date={} amount={}",
                sectorCode, exitDate, amountCharged, lastFailure);
    }

    private void sleepBeforeRetry() {
        if (retryDelayMs == 0) {
            return;
        }
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}

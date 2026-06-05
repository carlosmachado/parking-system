package br.com.cmachado.parkingsystem.application.revenue.impl;

import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenue;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import br.com.cmachado.parkingsystem.domain.model.vehicle.events.VehicleExited;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Updates the daily revenue when a vehicle exits. Runs asynchronously after the exit
 * transaction commits, in its own transaction, so revenue bookkeeping never blocks or
 * rolls back the webhook request.
 */
@Component
public class RevenueAsyncListener {

    private final DailyRevenueRepository dailyRevenueRepository;

    public RevenueAsyncListener(DailyRevenueRepository dailyRevenueRepository) {
        this.dailyRevenueRepository = dailyRevenueRepository;
    }

    /**
     * Adds the charged amount to the sector's running total for the exit date, creating the
     * {@link DailyRevenue} record on first use. Zero/empty charges are skipped.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleVehicleExited(VehicleExited event) {
        if (event.getAmountCharged() == null || event.getAmountCharged().getAmount().signum() == 0) {
            return; // No revenue to add
        }

        DailyRevenue dailyRevenue = dailyRevenueRepository
                .findBySectorCodeAndDate(event.getSectorCode(), event.getExitDate())
                .orElseGet(() -> new DailyRevenue(event.getSectorCode(), event.getExitDate()));

        dailyRevenue.addRevenue(event.getAmountCharged());
        dailyRevenueRepository.save(dailyRevenue);
    }
}

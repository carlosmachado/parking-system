package br.com.cmachado.parkingsystem.application.revenue.impl;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenue;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Applies a single revenue increment in its own transaction. Kept separate from
 * {@link RevenueAsyncListener} so the listener can wrap this call in a retry loop:
 * retrying must restart the whole transaction, which only works across a bean boundary.
 */
@Service
public class RevenueUpdater {

    private final DailyRevenueRepository dailyRevenueRepository;

    public RevenueUpdater(DailyRevenueRepository dailyRevenueRepository) {
        this.dailyRevenueRepository = dailyRevenueRepository;
    }

    /**
     * Adds {@code amount} to the sector's running total for {@code date}, creating the
     * {@link DailyRevenue} record on first use. Runs in a new transaction so revenue
     * bookkeeping is isolated from the webhook request that triggered it.
     *
     * <p>When the row exists it is taken under a pessimistic write lock, so concurrent exits
     * serialize on the counter and no increment is lost. The first exit of the day inserts the
     * row; if two exits race that insert, one hits the {@code (sector_code, date)} unique
     * constraint and the caller retries — by then the row exists and takes the locked path.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addRevenue(SectorCode sectorCode, LocalDate date, Money amount) {
        DailyRevenue dailyRevenue = dailyRevenueRepository
                .findWithLockBySectorCodeAndDate(sectorCode, date)
                .orElseGet(() -> new DailyRevenue(sectorCode, date));

        dailyRevenue.addRevenue(amount);
        dailyRevenueRepository.save(dailyRevenue);
    }
}

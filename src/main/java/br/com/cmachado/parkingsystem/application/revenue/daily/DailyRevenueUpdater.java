package br.com.cmachado.parkingsystem.application.revenue.daily;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenue;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import com.github.f4b6a3.ulid.UlidCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Applies a single revenue increment in its own transaction, isolated from the webhook request
 * that triggered it.
 */
@Service
public class DailyRevenueUpdater {

    private final DailyRevenueRepository dailyRevenueRepository;

    public DailyRevenueUpdater(DailyRevenueRepository dailyRevenueRepository) {
        this.dailyRevenueRepository = dailyRevenueRepository;
    }

    /**
     * Adds {@code amount} to the sector's running total for {@code date}, creating the
     * {@link DailyRevenue} row on first use. Implemented as one atomic
     * {@code INSERT … ON DUPLICATE KEY UPDATE}, so concurrent exits never lose an increment.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addRevenue(SectorCode sectorCode, LocalDate date, Money amount) {
        dailyRevenueRepository.upsertAddRevenue(
                newId(), sectorCode.getCode(), date, amount.getAmount());
    }

    /** New binary UUID for the insert branch, matching Hibernate's BINARY(16) layout (MSB then LSB). */
    private static byte[] newId() {
        UUID uuid = UlidCreator.getMonotonicUlid().toUuid();
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }
}

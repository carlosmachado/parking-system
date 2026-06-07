package br.com.cmachado.parkingsystem.domain.model.revenue;

import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyRevenueRepository extends JpaRepository<DailyRevenue, DailyRevenueId> {

    Optional<DailyRevenue> findBySectorCodeAndDate(SectorCode sectorCode, LocalDate date);

    List<DailyRevenue> findByDate(LocalDate date);

    /**
     * Atomically adds {@code amount} to the {@code (sectorCode, date)} row, inserting it on the
     * first exit of the day. A single {@code INSERT … ON DUPLICATE KEY UPDATE} statement: InnoDB
     * serializes concurrent upserts on the row, so increments are never lost and no retry is
     * needed. {@code id} is the binary UUID for the insert case (ignored when the row exists).
     */
    @Modifying
    @Query(value = """
            INSERT INTO daily_revenue (id, sector_code, date, total_amount, version, created_at, updated_at)
            VALUES (:id, :sectorCode, :date, :amount, 0, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                total_amount = total_amount + :amount,
                version = version + 1,
                updated_at = NOW()
            """, nativeQuery = true)
    void upsertAddRevenue(@Param("id") byte[] id,
                          @Param("sectorCode") String sectorCode,
                          @Param("date") LocalDate date,
                          @Param("amount") BigDecimal amount);
}

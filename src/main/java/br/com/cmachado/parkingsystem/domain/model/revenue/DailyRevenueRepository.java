package br.com.cmachado.parkingsystem.domain.model.revenue;

import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyRevenueRepository extends JpaRepository<DailyRevenue, DailyRevenueId> {
    Optional<DailyRevenue> findBySectorCodeAndDate(SectorCode sectorCode, LocalDate date);
    List<DailyRevenue> findByDate(LocalDate date);

    /**
     * Locks the row for update so concurrent exits in the same sector on the same day
     * serialize on the daily-revenue counter instead of racing and losing increments.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DailyRevenue> findWithLockBySectorCodeAndDate(SectorCode sectorCode, LocalDate date);
}

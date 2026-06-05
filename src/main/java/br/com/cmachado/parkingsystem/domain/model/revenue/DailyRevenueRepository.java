package br.com.cmachado.parkingsystem.domain.model.revenue;

import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyRevenueRepository extends JpaRepository<DailyRevenue, DailyRevenueId> {
    Optional<DailyRevenue> findBySectorCodeAndDate(SectorCode sectorCode, LocalDate date);
    List<DailyRevenue> findByDate(LocalDate date);
}

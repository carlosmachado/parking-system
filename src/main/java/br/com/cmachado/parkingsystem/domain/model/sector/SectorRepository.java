package br.com.cmachado.parkingsystem.domain.model.sector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, SectorId> {
    Optional<Sector> findByCode(SectorCode code);

    @Query("SELECT MIN(s.basePrice.amount) FROM Sector s")
    Optional<BigDecimal> findMinBasePrice();
}

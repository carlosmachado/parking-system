package br.com.cmachado.parkingsystem.domain.model.sector;

import br.com.cmachado.parkingsystem.infrastructure.cache.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, SectorId> {

    @Cacheable(value = CacheConfig.SECTORS, key = "#code.code")
    Optional<Sector> findByCode(SectorCode code);

    @Cacheable(CacheConfig.SECTOR_MIN_BASE_PRICE)
    @Query("SELECT MIN(s.basePrice.amount) FROM Sector s")
    Optional<BigDecimal> findMinBasePrice();
}

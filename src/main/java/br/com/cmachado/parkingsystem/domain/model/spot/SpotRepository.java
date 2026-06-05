package br.com.cmachado.parkingsystem.domain.model.spot;

import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpotRepository extends JpaRepository<Spot, SpotId> {
    long countByOccupiedTrue();
    List<Spot> findByOccupiedFalse();
    List<Spot> findBySectorCodeAndOccupiedFalse(SectorCode sectorCode);
    Optional<Spot> findByExternalId(Long externalId);
}

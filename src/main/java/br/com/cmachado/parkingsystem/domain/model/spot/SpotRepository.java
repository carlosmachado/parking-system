package br.com.cmachado.parkingsystem.domain.model.spot;

import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpotRepository extends JpaRepository<Spot, Long> {
    long countByOccupiedTrue();
    List<Spot> findByOccupiedFalse();
    List<Spot> findBySectorCodeAndOccupiedFalse(SectorCode sectorCode);
}

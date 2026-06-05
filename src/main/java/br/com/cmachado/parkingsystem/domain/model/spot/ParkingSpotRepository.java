package br.com.cmachado.parkingsystem.domain.model.spot;

import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, ParkingSpotId> {
    long countByOccupiedTrue();
    List<ParkingSpot> findByOccupiedFalse();
    List<ParkingSpot> findBySectorCodeAndOccupiedFalse(SectorCode sectorCode);
    List<ParkingSpot> findByOccupiedFalseAndSectorCodeIn(Collection<SectorCode> sectorCodes);
    Optional<ParkingSpot> findByExternalId(Long externalId);
}

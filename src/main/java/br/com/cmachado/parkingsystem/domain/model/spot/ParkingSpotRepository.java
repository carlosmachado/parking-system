package br.com.cmachado.parkingsystem.domain.model.spot;

import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, ParkingSpotId> {
    long countByOccupiedTrue();

    @Query("SELECT (1.0 * SUM(CASE WHEN p.occupied = true THEN 1 ELSE 0 END)) / NULLIF(COUNT(p), 0) FROM ParkingSpot p")
    Double findOccupancyRate();

    boolean existsByOccupiedFalseAndSectorCodeIn(Collection<SectorCode> sectorCodes);

    Optional<ParkingSpot> findByExternalId(Long externalId);

    Optional<ParkingSpot> findByLocation(GeoLocation location);
}

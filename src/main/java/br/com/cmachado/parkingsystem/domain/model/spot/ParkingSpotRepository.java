package br.com.cmachado.parkingsystem.domain.model.spot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, ParkingSpotId> {
    @Query("SELECT COALESCE((1.0 * SUM(CASE WHEN p.occupied = true THEN 1 ELSE 0 END)) / NULLIF(COUNT(p), 0), 0.0) FROM ParkingSpot p")
    Double findOccupancyRate();

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
              FROM ParkingSpot p, Sector s
             WHERE p.sectorCode = s.code
               AND p.occupied = false
               AND ( (s.openHour <= s.closeHour AND :time BETWEEN s.openHour AND s.closeHour)
                  OR (s.openHour > s.closeHour AND (:time >= s.openHour OR :time <= s.closeHour)) )
            """)
    boolean existsAvailableSpotInOpenSector(LocalTime time);

    Optional<ParkingSpot> findByExternalId(Long externalId);

    Optional<ParkingSpot> findByLocation(GeoLocation location);
}

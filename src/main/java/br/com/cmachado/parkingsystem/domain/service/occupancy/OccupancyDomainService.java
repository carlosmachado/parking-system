package br.com.cmachado.parkingsystem.domain.service.occupancy;

import br.com.cmachado.parkingsystem.domain.model.sector.OccupancyRate;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.shared.DomainService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Domain service for garage occupancy concerns: whether the garage is full, the current
 * occupancy rate (used to pick the pricing strategy), and locating the nearest free spot
 * to a vehicle's reported position.
 */
@DomainService
@Service
public class OccupancyDomainService {

    public boolean isGarageFull(int totalSpots, int occupiedSpots) {
        return totalSpots > 0 && occupiedSpots >= totalSpots;
    }

    public OccupancyRate calculateOccupancyRate(int totalSpots, int occupiedSpots) {
        if (totalSpots == 0) {
            return OccupancyRate.of(0.0, LocalDateTime.now());
        }
        double rate = (double) occupiedSpots / totalSpots;
        return OccupancyRate.of(Math.min(1.0, rate), LocalDateTime.now());
    }

    public Optional<ParkingSpot> findNearestAvailableSpot(List<ParkingSpot> spots, GeoLocation currentLoc) {
        if (spots == null || spots.isEmpty()) {
            return Optional.empty();
        }
        if (currentLoc == null) {
            return Optional.of(spots.get(0));
        }

        ParkingSpot nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (ParkingSpot spot : spots) {
            double distance = spot.getLocation().calculateDistance(currentLoc);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = spot;
            }
        }
        return Optional.ofNullable(nearest);
    }
}

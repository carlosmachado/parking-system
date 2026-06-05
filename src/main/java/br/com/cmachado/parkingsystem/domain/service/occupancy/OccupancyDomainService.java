package br.com.cmachado.parkingsystem.domain.service.occupancy;

import br.com.cmachado.parkingsystem.domain.model.garage.OccupancyRate;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.Spot;
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
            return new OccupancyRate(0.0, LocalDateTime.now());
        }
        double rate = (double) occupiedSpots / totalSpots;
        // Cap at 1.0 just in case
        return new OccupancyRate(Math.min(1.0, rate), LocalDateTime.now());
    }

    public Optional<Spot> findNearestAvailableSpot(List<Spot> availableSpots, GeoLocation currentLoc) {
        if (availableSpots == null || availableSpots.isEmpty()) {
            return Optional.empty();
        }
        if (currentLoc == null) {
            // If we don't know where the car is, just return the first available
            return Optional.of(availableSpots.get(0));
        }

        Spot nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Spot spot : availableSpots) {
            double distance = spot.getLocation().calculateDistance(currentLoc);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = spot;
            }
        }
        return Optional.ofNullable(nearest);
    }
}

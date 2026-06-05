package br.com.cmachado.parkingsystem.domain.service.occupancy;

import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OccupancyDomainServiceTest {

    private final OccupancyDomainService service = new OccupancyDomainService();

    @Test
    void garageIsFullOnlyWhenCapacityExistsAndOccupiedReachesTotal() {
        assertFalse(service.isGarageFull(0, 0));
        assertFalse(service.isGarageFull(10, 9));
        assertTrue(service.isGarageFull(10, 10));
        assertTrue(service.isGarageFull(10, 11));
    }

    @Test
    void calculatesOccupancyRateAndCapsOverOccupiedGarage() {
        assertEquals(0.0, service.calculateOccupancyRate(0, 0).getRate());
        assertEquals(0.25, service.calculateOccupancyRate(4, 1).getRate());
        assertEquals(1.0, service.calculateOccupancyRate(4, 4).getRate());
        assertEquals(1.0, service.calculateOccupancyRate(4, 5).getRate());
    }

    @Test
    void nearestSpotReturnsEmptyForNullOrEmptyCandidates() {
        assertTrue(service.findNearestAvailableSpot(null, new GeoLocation(0.0, 0.0)).isEmpty());
        assertTrue(service.findNearestAvailableSpot(List.of(), new GeoLocation(0.0, 0.0)).isEmpty());
    }

    @Test
    void nearestSpotReturnsFirstCandidateWhenCurrentLocationIsUnknown() {
        ParkingSpot first = spot(1L, 10.0, 10.0);
        ParkingSpot second = spot(2L, 0.0, 0.0);

        assertSame(first, service.findNearestAvailableSpot(List.of(first, second), null).orElseThrow());
    }

    @Test
    void nearestSpotSelectsCandidateClosestToCurrentLocation() {
        ParkingSpot far = spot(1L, 50.0, 50.0);
        ParkingSpot near = spot(2L, 10.1, 10.1);
        ParkingSpot middle = spot(3L, 11.0, 11.0);

        ParkingSpot selected = service.findNearestAvailableSpot(
                List.of(far, near, middle), new GeoLocation(10.0, 10.0)).orElseThrow();

        assertSame(near, selected);
    }

    private ParkingSpot spot(Long externalId, double lat, double lng) {
        return ParkingSpot.register(externalId, new SectorCode("A"), new GeoLocation(lat, lng));
    }
}

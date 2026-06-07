package br.com.cmachado.parkingsystem.domain.model.parkingspot;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleParked;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.CantParkSessionException;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.ParkingSpotOccupiedException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.events.ParkingSpotRegistered;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.events.ParkingSpotOccupied;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.events.ParkingSpotReleased;
import br.com.cmachado.parkingsystem.fixtures.ParkingSessionFixture;
import br.com.cmachado.parkingsystem.fixtures.ParkingSpotFixture;
import org.junit.jupiter.api.Test;

import static br.com.cmachado.parkingsystem.support.DomainEventAssertions.assertHasEvent;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkingSpotTest {

    @Test
    void registerCreatesFreeSpotAndRegistersEvent() {
        // arrange / act
        ParkingSpot spot = ParkingSpotFixture.aSpot().build();

        // assert
        assertFalse(spot.isOccupied(), "newly registered spot must be free");
        assertHasEvent(spot, ParkingSpotRegistered.class);
    }

    @Test
    void parkOccupiesSpotParksSessionAndRegistersEventsOnBothAggregates() {
        // arrange
        ParkingSpot spot = ParkingSpotFixture.aSpot().build();
        ParkingSession session = ParkingSessionFixture.aSession().build();

        // act
        spot.park(session);

        // assert
        assertTrue(spot.isOccupied(), "spot must be occupied after park");
        assertHasEvent(spot, ParkingSpotOccupied.class);
        assertHasEvent(session, VehicleParked.class);
    }

    @Test
    void parkRejectsSessionNotInEnteredStatus() {
        // arrange
        ParkingSpot first = ParkingSpotFixture.aSpot().withExternalId(1L).build();
        ParkingSession session = ParkingSessionFixture.aSession().build();
        first.park(session); // session now PARKED
        ParkingSpot second = ParkingSpotFixture.aSpot().withExternalId(2L).build();

        // act / assert
        assertThrows(CantParkSessionException.class, () -> second.park(session),
                "must not park a session that is not ENTERED");
    }

    @Test
    void parkRejectsAlreadyOccupiedSpot() {
        // arrange
        ParkingSpot spot = ParkingSpotFixture.aSpot().build();
        spot.park(ParkingSessionFixture.aSession().withPlate("AAA1111").build());
        ParkingSession another = ParkingSessionFixture.aSession().withPlate("BBB2222").build();

        // act / assert
        assertThrows(ParkingSpotOccupiedException.class, () -> spot.park(another),
                "must not park on an already-occupied spot");
    }

    @Test
    void releaseFreesSpotAndRegistersEvent() {
        // arrange
        ParkingSpot spot = ParkingSpotFixture.aSpot().build();
        spot.park(ParkingSessionFixture.aSession().build());

        // act
        spot.release();

        // assert
        assertFalse(spot.isOccupied(), "spot must be free after release");
        assertHasEvent(spot, ParkingSpotReleased.class);
    }
}

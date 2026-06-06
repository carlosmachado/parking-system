package br.com.cmachado.parkingsystem.domain.model.parkingspot;

import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.fixtures.ParkingSessionFixture;
import br.com.cmachado.parkingsystem.fixtures.ParkingSpotFixture;
import br.com.cmachado.parkingsystem.fixtures.SectorFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class ParkingSpotRepositoryTest {

    private static final LocalTime NOON = LocalTime.NOON;

    @Autowired
    private ParkingSpotRepository spotRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Test
    void availableSpotInOpenSectorExists() {
        // arrange
        sectorRepository.save(SectorFixture.aSector().withCode("SEC-A").withHours(LocalTime.of(8, 0), LocalTime.of(18, 0)).build());
        spotRepository.save(ParkingSpotFixture.aSpot().withSector("SEC-A").build());

        // act
        boolean exists = spotRepository.existsAvailableSpotInOpenSector(NOON);

        // assert
        assertTrue(exists, "a free spot in an open sector must be found");
    }

    @Test
    void freeSpotOnlyInClosedSectorDoesNotExist() {
        // arrange — sector open 00:00–01:00 only, closed at noon
        sectorRepository.save(SectorFixture.aSector().withCode("SEC-B").withHours(LocalTime.of(0, 0), LocalTime.of(1, 0)).build());
        spotRepository.save(ParkingSpotFixture.aSpot().withSector("SEC-B").build());

        // act
        boolean exists = spotRepository.existsAvailableSpotInOpenSector(NOON);

        // assert
        assertFalse(exists, "a free spot in a closed sector must not count as available");
    }

    @Test
    void availableSpotInWrapAroundSectorOpenLateNightExists() {
        // arrange — sector open 22:00 → 06:00 wraps past midnight
        sectorRepository.save(SectorFixture.aSector().withCode("SEC-W").withHours(LocalTime.of(22, 0), LocalTime.of(6, 0)).build());
        spotRepository.save(ParkingSpotFixture.aSpot().withSector("SEC-W").build());

        // act / assert
        assertFalse(spotRepository.existsAvailableSpotInOpenSector(NOON), "noon is outside the wrapped window");
        assertTrue(spotRepository.existsAvailableSpotInOpenSector(LocalTime.of(23, 0)), "23:00 is inside the wrapped window");
        assertTrue(spotRepository.existsAvailableSpotInOpenSector(LocalTime.of(2, 0)), "02:00 is inside the wrapped window");
    }

    @Test
    void occupiedSpotInOpenSectorDoesNotExist() {
        // arrange — the only spot in the open sector is occupied
        sectorRepository.save(SectorFixture.aSector().withCode("SEC-C").withHours(LocalTime.of(8, 0), LocalTime.of(18, 0)).build());
        ParkingSpot spot = ParkingSpotFixture.aSpot().withSector("SEC-C").build();
        spot.park(ParkingSessionFixture.aSession().build());
        spotRepository.save(spot);

        // act
        boolean exists = spotRepository.existsAvailableSpotInOpenSector(NOON);

        // assert
        assertFalse(exists, "an occupied spot must not count as available");
    }
}

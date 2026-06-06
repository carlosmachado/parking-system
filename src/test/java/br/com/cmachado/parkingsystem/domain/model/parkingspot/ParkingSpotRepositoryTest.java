package br.com.cmachado.parkingsystem.domain.model.parkingspot;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
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
        SectorCode code = SectorCode.of("SEC-A");
        sectorRepository.save(openSector(code));
        spotRepository.save(ParkingSpot.register(1L, code, GeoLocation.of(10.0, 10.0)));

        boolean exists = spotRepository.existsAvailableSpotInOpenSector(NOON);

        assertTrue(exists);
    }

    @Test
    void freeSpotOnlyInClosedSectorDoesNotExist() {
        SectorCode code = SectorCode.of("SEC-B");
        sectorRepository.save(closedSector(code));
        spotRepository.save(ParkingSpot.register(1L, code, GeoLocation.of(10.0, 10.0)));

        boolean exists = spotRepository.existsAvailableSpotInOpenSector(NOON);

        assertFalse(exists);
    }

    @Test
    void availableSpotInWrapAroundSectorOpenLateNightExists() {
        // Sector open 22:00 → 06:00 (wraps past midnight); NOON is outside, 23:00 is inside.
        SectorCode code = SectorCode.of("SEC-W");
        sectorRepository.save(Sector.register(code, Money.of(10.0), 10,
                LocalTime.of(22, 0), LocalTime.of(6, 0), 1440));
        spotRepository.save(ParkingSpot.register(1L, code, GeoLocation.of(10.0, 10.0)));

        assertFalse(spotRepository.existsAvailableSpotInOpenSector(NOON));
        assertTrue(spotRepository.existsAvailableSpotInOpenSector(LocalTime.of(23, 0)));
        assertTrue(spotRepository.existsAvailableSpotInOpenSector(LocalTime.of(2, 0)));
    }

    @Test
    void occupiedSpotInOpenSectorDoesNotExist() {
        SectorCode code = SectorCode.of("SEC-C");
        sectorRepository.save(openSector(code));
        ParkingSpot spot = ParkingSpot.register(1L, code, GeoLocation.of(10.0, 10.0));
        spot.park(ParkingSession.enter(LicensePlate.of("ABC1234"), LocalDateTime.now()));
        spotRepository.save(spot);

        boolean exists = spotRepository.existsAvailableSpotInOpenSector(NOON);

        assertFalse(exists);
    }

    private Sector openSector(SectorCode code) {
        return Sector.register(code, Money.of(10.0), 10,
                LocalTime.of(8, 0), LocalTime.of(18, 0), 1440);
    }

    private Sector closedSector(SectorCode code) {
        return Sector.register(code, Money.of(10.0), 10,
                LocalTime.of(0, 0), LocalTime.of(1, 0), 1440);
    }
}

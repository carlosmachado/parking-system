package br.com.cmachado.parkingsystem.domain.model.sector;

import br.com.cmachado.parkingsystem.domain.model.sector.events.SectorCreated;
import br.com.cmachado.parkingsystem.fixtures.SectorFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static br.com.cmachado.parkingsystem.support.DomainEventAssertions.assertHasEvent;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectorTest {

    @Test
    void registerCreatesSectorAndRegistersEvent() {
        // arrange / act
        Sector sector = SectorFixture.aSector().withCode("A").build();

        // assert
        assertHasEvent(sector, SectorCreated.class);
    }

    @Test
    void daytimeWindowIsOpenWithinBoundsInclusive() {
        // arrange
        Sector sector = sectorOpen(LocalTime.of(8, 0), LocalTime.of(18, 0));

        // act / assert
        assertTrue(sector.isOpen(LocalTime.of(8, 0)), "open hour is inclusive");
        assertTrue(sector.isOpen(LocalTime.NOON), "midday is within the window");
        assertTrue(sector.isOpen(LocalTime.of(18, 0)), "close hour is inclusive");
        assertFalse(sector.isOpen(LocalTime.of(7, 59)), "one minute before open is closed");
        assertFalse(sector.isOpen(LocalTime.of(18, 1)), "one minute after close is closed");
    }

    @Test
    void wrapAroundWindowIsOpenAcrossMidnight() {
        // arrange — 22:00 → 06:00 wraps past midnight
        Sector sector = sectorOpen(LocalTime.of(22, 0), LocalTime.of(6, 0));

        // act / assert
        assertTrue(sector.isOpen(LocalTime.of(22, 0)), "open hour is inclusive");
        assertTrue(sector.isOpen(LocalTime.of(23, 30)), "late evening is within the window");
        assertTrue(sector.isOpen(LocalTime.MIDNIGHT), "midnight is within the wrapped window");
        assertTrue(sector.isOpen(LocalTime.of(6, 0)), "close hour is inclusive");
        assertFalse(sector.isOpen(LocalTime.of(6, 1)), "one minute after close is closed");
        assertFalse(sector.isOpen(LocalTime.NOON), "midday is outside the wrapped window");
    }

    @Test
    void nearlyAllDayWindowSimulatorStyleIsOpen() {
        // arrange — simulator sends open 03:00 / close 02:59 → effectively 24h
        Sector sector = sectorOpen(LocalTime.of(3, 0), LocalTime.of(2, 59));

        // act / assert
        assertTrue(sector.isOpen(LocalTime.of(3, 0)), "open hour is inclusive");
        assertTrue(sector.isOpen(LocalTime.NOON), "midday is within the window");
        assertTrue(sector.isOpen(LocalTime.of(2, 59)), "close hour is inclusive");
        assertTrue(sector.isOpen(LocalTime.MIDNIGHT), "midnight is within the wrapped window");
    }

    private Sector sectorOpen(LocalTime openHour, LocalTime closeHour) {
        return SectorFixture.aSector().withHours(openHour, closeHour).build();
    }
}

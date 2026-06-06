package br.com.cmachado.parkingsystem.domain.model.sector;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectorTest {

    @Test
    void daytimeWindowIsOpenWithinBoundsInclusive() {
        Sector sector = sector(LocalTime.of(8, 0), LocalTime.of(18, 0));

        assertTrue(sector.isOpen(LocalTime.of(8, 0)));
        assertTrue(sector.isOpen(LocalTime.NOON));
        assertTrue(sector.isOpen(LocalTime.of(18, 0)));
        assertFalse(sector.isOpen(LocalTime.of(7, 59)));
        assertFalse(sector.isOpen(LocalTime.of(18, 1)));
    }

    @Test
    void wrapAroundWindowIsOpenAcrossMidnight() {
        // 22:00 → 06:00 wraps past midnight
        Sector sector = sector(LocalTime.of(22, 0), LocalTime.of(6, 0));

        assertTrue(sector.isOpen(LocalTime.of(22, 0)));
        assertTrue(sector.isOpen(LocalTime.of(23, 30)));
        assertTrue(sector.isOpen(LocalTime.MIDNIGHT));
        assertTrue(sector.isOpen(LocalTime.of(6, 0)));
        assertFalse(sector.isOpen(LocalTime.of(6, 1)));
        assertFalse(sector.isOpen(LocalTime.NOON));
    }

    @Test
    void nearlyAllDayWindowSimulatorStyleIsOpen() {
        // Simulator sends open 03:00 / close 02:59 → effectively 24h
        Sector sector = sector(LocalTime.of(3, 0), LocalTime.of(2, 59));

        assertTrue(sector.isOpen(LocalTime.of(3, 0)));
        assertTrue(sector.isOpen(LocalTime.NOON));
        assertTrue(sector.isOpen(LocalTime.of(2, 59)));
        assertTrue(sector.isOpen(LocalTime.MIDNIGHT));
    }

    private Sector sector(LocalTime openHour, LocalTime closeHour) {
        return Sector.register(SectorCode.of("A"), Money.of(10.0), 10, openHour, closeHour, 1440);
    }
}

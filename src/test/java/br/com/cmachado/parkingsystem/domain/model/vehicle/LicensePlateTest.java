package br.com.cmachado.parkingsystem.domain.model.vehicle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicensePlateTest {

    @Test
    void normalizesToUpperCaseAlphanumeric() {
        assertEquals("ZUL0001", new LicensePlate(" zul-0001 ").getPlate());
    }

    @Test
    void equalPlatesAreValueEqual() {
        assertTrue(new LicensePlate("abc1234").sameValueAs(new LicensePlate("ABC-1234")));
    }

    @Test
    void rejectsNullPlate() {
        assertThrows(IllegalArgumentException.class, () -> new LicensePlate(null));
    }

    @Test
    void rejectsBlankPlate() {
        assertThrows(IllegalArgumentException.class, () -> new LicensePlate("   "));
    }
}

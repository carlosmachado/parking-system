package br.com.cmachado.parkingsystem.domain.model.parkingsession;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicensePlateTest {

    @Test
    void normalizesToUpperCaseAlphanumeric() {
        assertEquals("ZUL0001", LicensePlate.of(" zul-0001 ").getPlate());
    }

    @Test
    void equalPlatesAreValueEqual() {
        assertTrue(LicensePlate.of("abc1234").sameValueAs(LicensePlate.of("ABC-1234")));
    }

    @Test
    void rejectsNullPlate() {
        assertThrows(NullPointerException.class, () -> LicensePlate.of(null));
    }

    @Test
    void rejectsBlankPlate() {
        assertThrows(IllegalArgumentException.class, () -> LicensePlate.of("   "));
    }
}

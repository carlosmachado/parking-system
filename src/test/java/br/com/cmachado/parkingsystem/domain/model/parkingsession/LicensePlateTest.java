package br.com.cmachado.parkingsystem.domain.model.parkingsession;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicensePlateTest {

    @Test
    void normalizesToUpperCaseAlphanumeric() {
        // act
        LicensePlate plate = LicensePlate.of(" zul-0001 ");

        // assert
        assertEquals("ZUL0001", plate.getPlate(), "plate must be trimmed, upper-cased, alphanumeric only");
    }

    @Test
    void equalPlatesAreValueEqual() {
        // arrange / act / assert
        assertTrue(LicensePlate.of("abc1234").sameValueAs(LicensePlate.of("ABC-1234")),
                "plates differing only by case/separators must be value-equal");
    }

    @Test
    void rejectsNullPlate() {
        // act / assert
        assertThrows(NullPointerException.class, () -> LicensePlate.of(null),
                "null plate must be rejected");
    }

    @Test
    void rejectsBlankPlate() {
        // act / assert
        assertThrows(IllegalArgumentException.class, () -> LicensePlate.of("   "),
                "blank plate must be rejected");
    }
}

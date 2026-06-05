package br.com.cmachado.parkingsystem.domain.model.common.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void keepsScaleOfTwo() {
        assertEquals(new BigDecimal("10.00"), Money.of("10").getAmount());
    }

    @Test
    void addsAmounts() {
        assertTrue(Money.of("10.00").add(Money.of("5.50")).sameValueAs(Money.of("15.50")));
    }

    @Test
    void subtractsAmounts() {
        assertTrue(Money.of("10.00").subtract(Money.of("4.00")).sameValueAs(Money.of("6.00")));
    }

    @Test
    void multipliesByHours() {
        assertTrue(Money.of("10.00").multiply(3L).sameValueAs(Money.of("30.00")));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(MoneyInvalidException.class, () -> Money.of("-1.00"));
    }

    @Test
    void rejectsSubtractionResultingInNegative() {
        assertThrows(MoneyInvalidException.class, () -> Money.of("1.00").subtract(Money.of("2.00")));
    }

    @Test
    void zeroIsValueComparable() {
        assertTrue(Money.ZERO.sameValueAs(Money.of("0")));
    }
}

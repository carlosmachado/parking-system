package br.com.cmachado.parkingsystem.domain.model.common.money;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    // ── construction ────────────────────────────────────────────────────────

    @Test
    void keepsScaleOfTwo() {
        // act / assert
        assertEquals(new BigDecimal("10.00"), Money.of("10").getAmount(), "amount must be scaled to 2 decimals");
    }

    @Test
    void roundsHalfUp() {
        // act / assert
        assertEquals(new BigDecimal("10.56"), Money.of("10.555").getAmount(), "10.555 rounds half-up to 10.56");
    }

    @Test
    void doubleFactoryKeepsPrecision() {
        // act / assert
        assertEquals(new BigDecimal("10.10"), Money.of(10.1).getAmount(), "double 10.1 maps to 10.10");
    }

    @Test
    void rejectsNullBigDecimal() {
        // act / assert
        assertThrows(MoneyInvalidException.class, () -> Money.of((BigDecimal) null), "null BigDecimal must be rejected");
    }

    @Test
    void rejectsNullString() {
        // act / assert
        assertThrows(Exception.class, () -> Money.of((String) null), "null string must be rejected");
    }

    @Test
    void rejectsNegativeAmount() {
        // act / assert
        assertThrows(MoneyInvalidException.class, () -> Money.of("-1.00"), "negative amount must be rejected");
    }

    @Test
    void zeroIsValid() {
        // act / assert
        assertEquals(new BigDecimal("0.00"), Money.ZERO.getAmount(), "ZERO must be 0.00");
    }

    // ── arithmetic ──────────────────────────────────────────────────────────

    @Test
    void addsAmounts() {
        // act / assert
        assertTrue(Money.of("10.00").add(Money.of("5.50")).sameValueAs(Money.of("15.50")), "10.00 + 5.50 = 15.50");
    }

    @Test
    void addWithZeroUnchanged() {
        // act / assert
        assertTrue(Money.of("10.00").add(Money.ZERO).sameValueAs(Money.of("10.00")), "adding zero is identity");
    }

    @Test
    void subtractsAmounts() {
        // act / assert
        assertTrue(Money.of("10.00").subtract(Money.of("4.00")).sameValueAs(Money.of("6.00")), "10.00 - 4.00 = 6.00");
    }

    @Test
    void rejectsSubtractionResultingInNegative() {
        // act / assert
        assertThrows(MoneyInvalidException.class, () -> Money.of("1.00").subtract(Money.of("2.00")),
                "result below zero must be rejected");
    }

    @Test
    void subtractSameAmountGivesZero() {
        // act / assert
        assertTrue(Money.of("5.00").subtract(Money.of("5.00")).sameValueAs(Money.ZERO), "x - x = 0");
    }

    @Test
    void multipliesByHours() {
        // act / assert
        assertTrue(Money.of("10.00").multiply(3L).sameValueAs(Money.of("30.00")), "10.00 * 3 = 30.00");
    }

    @Test
    void multiplyByZeroLongGivesZero() {
        // act / assert
        assertTrue(Money.of("10.00").multiply(0L).sameValueAs(Money.ZERO), "x * 0 = 0");
    }

    @Test
    void multipliesBySurchargeDouble() {
        // act / assert
        assertTrue(Money.of("10.00").multiply(1.10).sameValueAs(Money.of("11.00")), "10.00 * 1.10 = 11.00");
    }

    @Test
    void multipliesByDiscountDouble() {
        // act / assert
        assertTrue(Money.of("10.00").multiply(0.90).sameValueAs(Money.of("9.00")), "10.00 * 0.90 = 9.00");
    }

    // ── equality ────────────────────────────────────────────────────────────

    @Test
    void zeroIsValueComparable() {
        // act / assert
        assertTrue(Money.ZERO.sameValueAs(Money.of("0")), "ZERO equals 0 by value");
    }

    @Test
    void sameValueAsNullReturnsFalse() {
        // act / assert
        assertFalse(Money.of("10.00").sameValueAs(null), "value equality with null is false");
    }

    @Test
    void differentScaleStillEqual() {
        // act / assert
        assertTrue(Money.of("10.0").sameValueAs(Money.of("10.00")), "scale must not affect value equality");
    }

    @Test
    void equalsConsistentWithSameValueAs() {
        // arrange
        Money a = Money.of("5.00");
        Money b = Money.of("5.00");

        // act / assert
        assertEquals(a, b, "equal values must be equals()");
        assertEquals(a.hashCode(), b.hashCode(), "equal values must share hashCode");
    }

    @Test
    void toStringNotBlank() {
        // act / assert
        assertFalse(Money.of("10.00").toString().isBlank(), "toString must render something");
    }

    // ── DB hydration ─────────────────────────────────────────────────────────
    // Hibernate calls the protected no-arg constructor, then sets `amount` via
    // reflection. `monetaryAmount` is @Transient so it stays null until first use.

    @Test
    void lazyMonetaryAmountInitAfterHibernateHydration() throws Exception {
        // arrange
        Money hydrated = hydrateFromDb("25.50");

        // act / assert — monetaryAmount is null until getMonetaryAmount() lazy-inits it
        assertNotNull(hydrated.getMonetaryAmount(), "monetary amount must lazy-init after hydration");
        assertEquals(new BigDecimal("25.50"), hydrated.getAmount(), "hydrated amount");
        assertTrue(hydrated.sameValueAs(Money.of("25.50")), "hydrated value equality");
    }

    @Test
    void arithmeticWorksAfterHibernateHydration() throws Exception {
        // arrange
        Money hydrated = hydrateFromDb("10.00");

        // act / assert
        assertTrue(hydrated.add(Money.of("5.00")).sameValueAs(Money.of("15.00")), "add after hydration");
        assertTrue(hydrated.multiply(1.10).sameValueAs(Money.of("11.00")), "multiply after hydration");
    }

    @Test
    void toStringWorksAfterHibernateHydration() throws Exception {
        // arrange
        Money hydrated = hydrateFromDb("10.00");

        // act / assert
        assertFalse(hydrated.toString().isBlank(), "toString after hydration");
    }

    private static Money hydrateFromDb(String amount) throws Exception {
        Constructor<Money> ctor = Money.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        Money m = ctor.newInstance();
        Field amountField = Money.class.getDeclaredField("amount");
        amountField.setAccessible(true);
        amountField.set(m, new BigDecimal(amount));
        return m;
    }
}

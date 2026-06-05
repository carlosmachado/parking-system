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
        assertEquals(new BigDecimal("10.00"), Money.of("10").getAmount());
    }

    @Test
    void roundsHalfUp() {
        assertEquals(new BigDecimal("10.56"), Money.of("10.555").getAmount());
    }

    @Test
    void doubleFactoryKeepsPrecision() {
        assertEquals(new BigDecimal("10.10"), Money.of(10.1).getAmount());
    }

    @Test
    void rejectsNullBigDecimal() {
        assertThrows(MoneyInvalidException.class, () -> Money.of((BigDecimal) null));
    }

    @Test
    void rejectsNullString() {
        assertThrows(Exception.class, () -> Money.of((String) null));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(MoneyInvalidException.class, () -> Money.of("-1.00"));
    }

    @Test
    void zeroIsValid() {
        assertEquals(new BigDecimal("0.00"), Money.ZERO.getAmount());
    }

    // ── arithmetic ──────────────────────────────────────────────────────────

    @Test
    void addsAmounts() {
        assertTrue(Money.of("10.00").add(Money.of("5.50")).sameValueAs(Money.of("15.50")));
    }

    @Test
    void addWithZeroUnchanged() {
        assertTrue(Money.of("10.00").add(Money.ZERO).sameValueAs(Money.of("10.00")));
    }

    @Test
    void subtractsAmounts() {
        assertTrue(Money.of("10.00").subtract(Money.of("4.00")).sameValueAs(Money.of("6.00")));
    }

    @Test
    void rejectsSubtractionResultingInNegative() {
        assertThrows(MoneyInvalidException.class, () -> Money.of("1.00").subtract(Money.of("2.00")));
    }

    @Test
    void subtractSameAmountGivesZero() {
        assertTrue(Money.of("5.00").subtract(Money.of("5.00")).sameValueAs(Money.ZERO));
    }

    @Test
    void multipliesByHours() {
        assertTrue(Money.of("10.00").multiply(3L).sameValueAs(Money.of("30.00")));
    }

    @Test
    void multiplyByZeroLongGivesZero() {
        assertTrue(Money.of("10.00").multiply(0L).sameValueAs(Money.ZERO));
    }

    @Test
    void multipliesBySurchargeDouble() {
        // 10.00 * 1.10 = 11.00
        assertTrue(Money.of("10.00").multiply(1.10).sameValueAs(Money.of("11.00")));
    }

    @Test
    void multipliesByDiscountDouble() {
        // 10.00 * 0.90 = 9.00
        assertTrue(Money.of("10.00").multiply(0.90).sameValueAs(Money.of("9.00")));
    }

    // ── equality ────────────────────────────────────────────────────────────

    @Test
    void zeroIsValueComparable() {
        assertTrue(Money.ZERO.sameValueAs(Money.of("0")));
    }

    @Test
    void sameValueAsNullReturnsFalse() {
        assertFalse(Money.of("10.00").sameValueAs(null));
    }

    @Test
    void differentScaleStillEqual() {
        assertTrue(Money.of("10.0").sameValueAs(Money.of("10.00")));
    }

    @Test
    void equalsConsistentWithSameValueAs() {
        Money a = Money.of("5.00");
        Money b = Money.of("5.00");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toStringNotBlank() {
        assertFalse(Money.of("10.00").toString().isBlank());
    }

    // ── DB hydration ─────────────────────────────────────────────────────────
    // Hibernate calls the protected no-arg constructor, then sets `amount` via
    // reflection. `monetaryAmount` is @Transient so it stays null until first use.

    @Test
    void lazyMonetaryAmountInitAfterHibernateHydration() throws Exception {
        Money hydrated = hydrateFromDb("25.50");

        // monetaryAmount is null at this point — getMonetaryAmount() must lazy-init
        assertNotNull(hydrated.getMonetaryAmount());
        assertEquals(new BigDecimal("25.50"), hydrated.getAmount());
        assertTrue(hydrated.sameValueAs(Money.of("25.50")));
    }

    @Test
    void arithmeticWorksAfterHibernateHydration() throws Exception {
        Money hydrated = hydrateFromDb("10.00");
        assertTrue(hydrated.add(Money.of("5.00")).sameValueAs(Money.of("15.00")));
        assertTrue(hydrated.multiply(1.10).sameValueAs(Money.of("11.00")));
    }

    @Test
    void toStringWorksAfterHibernateHydration() throws Exception {
        Money hydrated = hydrateFromDb("10.00");
        assertFalse(hydrated.toString().isBlank());
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

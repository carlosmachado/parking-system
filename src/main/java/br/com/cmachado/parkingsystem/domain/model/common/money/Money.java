package br.com.cmachado.parkingsystem.domain.model.common.money;

import br.com.cmachado.parkingsystem.domain.shared.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable monetary value object backed by the JSR-354 Money API.
 *
 * <p>Amounts are kept at scale 2 (HALF_UP), are never negative, and arithmetic between
 * different currencies is rejected. Defaults to {@link Currency#BRL}.</p>
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money implements ValueObject<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    @Column(name = "amount")
    private BigDecimal amount;

    @Transient
    private Currency currency = Currency.BRL;

    @Transient
    private MonetaryAmount monetaryAmount;

    private Money(BigDecimal amount) {
        if (amount == null) {
            throw new MoneyInvalidException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new MoneyInvalidException("Amount cannot be negative");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.monetaryAmount = org.javamoney.moneta.Money.of(this.amount, currency.getMonetaCurrency());
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money add(Money other) {
        if (!this.currency.sameValueAs(other.currency)) {
            throw new MoneyInvalidException("Cannot add money with different currencies");
        }
        MonetaryAmount newAmount = getMonetaryAmount().add(other.getMonetaryAmount());
        return new Money(newAmount.getNumber().numberValue(BigDecimal.class));
    }

    public Money subtract(Money other) {
        if (!this.currency.sameValueAs(other.currency)) {
            throw new MoneyInvalidException("Cannot subtract money with different currencies");
        }
        MonetaryAmount newAmount = getMonetaryAmount().subtract(other.getMonetaryAmount());
        if (newAmount.isNegative()) {
            throw new MoneyInvalidException("Money cannot be negative after subtraction");
        }
        return new Money(newAmount.getNumber().numberValue(BigDecimal.class));
    }

    public Money multiply(long factor) {
        MonetaryAmount newAmount = getMonetaryAmount().multiply(factor);
        return new Money(newAmount.getNumber().numberValue(BigDecimal.class));
    }

    public Money multiply(double factor) {
        MonetaryAmount newAmount = getMonetaryAmount().multiply(factor);
        return new Money(newAmount.getNumber().numberValue(BigDecimal.class));
    }

    public MonetaryAmount getMonetaryAmount() {
        if (this.monetaryAmount == null) {
            this.monetaryAmount = org.javamoney.moneta.Money.of(this.amount, this.currency.getMonetaCurrency());
        }
        return this.monetaryAmount;
    }

    @Override
    public boolean sameValueAs(Money other) {
        return other != null && this.amount.compareTo(other.amount) == 0 && this.currency.sameValueAs(other.currency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return sameValueAs(money);
    }

    @Override
    public int hashCode() {
        return amount.hashCode() + currency.hashCode();
    }
    
    @Override
    public String toString() {
        return getMonetaryAmount().toString();
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
}

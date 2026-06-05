package br.com.cmachado.parkingsystem.domain.model.common.money;

import br.com.cmachado.parkingsystem.domain.shared.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Currency implements ValueObject<Currency> {

    @Column(name = "currency", length = 10)
    private String value;

    public static final Currency BRL = new Currency("BRL");

    private Currency(String value) {
        this.value = value;
    }

    public CurrencyUnit getMonetaCurrency() {
        return Monetary.getCurrency(this.value);
    }

    @Override
    public boolean sameValueAs(Currency other) {
        return other != null && this.value.equals(other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Currency currency = (Currency) o;
        return sameValueAs(currency);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}

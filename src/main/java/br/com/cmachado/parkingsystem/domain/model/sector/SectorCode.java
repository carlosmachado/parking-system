package br.com.cmachado.parkingsystem.domain.model.sector;

import br.com.cmachado.parkingsystem.domain.shared.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Value object identifying a sector by its code (e.g. "A"), normalized to upper case.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SectorCode implements ValueObject<SectorCode> {

    @Column(name = "code", length = 10, nullable = false, unique = true)
    private String code;

    private SectorCode(String code) {
        this.code = code.trim().toUpperCase();
    }

    public static SectorCode of(String code) {
        Objects.requireNonNull(code, "Sector code cannot be null");
        if (code.trim().isEmpty()) {
            throw new IllegalArgumentException("Sector code cannot be blank");
        }
        return new SectorCode(code);
    }

    @Override
    public boolean sameValueAs(SectorCode other) {
        return other != null && this.code.equals(other.code);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SectorCode that = (SectorCode) o;
        return sameValueAs(that);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }
}

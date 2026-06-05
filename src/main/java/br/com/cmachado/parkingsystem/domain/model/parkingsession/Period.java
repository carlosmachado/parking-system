package br.com.cmachado.parkingsystem.domain.model.parkingsession;

import br.com.cmachado.parkingsystem.domain.shared.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object for a parking interval (entry to exit). Exposes the stay duration in
 * minutes, used by the pricing strategies. Exit must not precede entry.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Period implements ValueObject<Period> {

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    public Period(LocalDateTime entryTime) {
        this.entryTime = Objects.requireNonNull(entryTime, "Entry time cannot be null");
    }

    public void setExitTime(LocalDateTime exitTime) {
        Objects.requireNonNull(exitTime, "Exit time cannot be null");
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("Exit time cannot be before entry time");
        }
        this.exitTime = exitTime;
    }

    public long getDurationInMinutes() {
        if (entryTime == null || exitTime == null) return 0;
        return Duration.between(entryTime, exitTime).toMinutes();
    }

    @Override
    public boolean sameValueAs(Period other) {
        if (other == null) return false;
        boolean entrySame = this.entryTime != null ? this.entryTime.equals(other.entryTime) : other.entryTime == null;
        boolean exitSame = this.exitTime != null ? this.exitTime.equals(other.exitTime) : other.exitTime == null;
        return entrySame && exitSame;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Period period = (Period) o;
        return sameValueAs(period);
    }

    @Override
    public int hashCode() {
        int result = entryTime != null ? entryTime.hashCode() : 0;
        result = 31 * result + (exitTime != null ? exitTime.hashCode() : 0);
        return result;
    }
}

package br.com.cmachado.parkingsystem.domain.model.parkingsession;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpotId;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleEntered;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleExited;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleParked;
import br.com.cmachado.parkingsystem.domain.shared.AggregateRootBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Aggregate root tracking a single vehicle's stay, from entry through parking to exit.
 *
 * <p>State advances only through {@link #enter}, {@link #park} and {@link #exit}, which
 * guard the {@link ParkingSessionStatus} transitions. On exit a {@link VehicleExited} domain
 * event is registered so daily revenue is updated asynchronously.</p>
 */
@Entity
@Table(name = "parking_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingSession extends AggregateRootBase<ParkingSession> {

    @EmbeddedId
    private ParkingSessionId id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Embedded
    private LicensePlate licensePlate;

    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "sector_code"))
    private SectorCode sectorCode;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "spot_id", columnDefinition = "BINARY(16)"))
    private ParkingSpotId spotId;

    @Embedded
    private Period period;

    @Column(name = "parked_time")
    private LocalDateTime parkedTime;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount_charged"))
    private Money amountCharged;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParkingSessionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ParkingSession(LicensePlate licensePlate, LocalDateTime entryTime) {
        this.id = ParkingSessionId.generate();
        this.licensePlate = licensePlate;
        this.period = new Period(entryTime);
        this.status = ParkingSessionStatus.ENTERED;
        registerEvent(new VehicleEntered(this));
    }

    /** Creates a new parking session in {@code ENTERED} status. */
    public static ParkingSession enter(LicensePlate plate, LocalDateTime entryTime) {
        return new ParkingSession(plate, entryTime);
    }

    /**
     * Assigns a spot and moves the session to {@code PARKED}.
     *
     * @throws IllegalStateException if the session is not in {@code ENTERED} status
     */
    public void park(ParkingSpotId spotId, SectorCode sectorCode, LocalDateTime parkedTime) {
        if (this.status != ParkingSessionStatus.ENTERED) {
            throw new IllegalStateException("Session must be in ENTERED status to park");
        }
        this.spotId = spotId;
        this.sectorCode = sectorCode;
        this.parkedTime = parkedTime;
        this.status = ParkingSessionStatus.PARKED;
        registerEvent(new VehicleParked(this));
    }

    /**
     * Records the exit time and final charge, moves the session to {@code EXITED} and
     * registers a {@link VehicleExited} domain event.
     *
     * @throws IllegalStateException if the session has already exited
     */
    public void exit(LocalDateTime exitTime, Money amount) {
        if (this.status == ParkingSessionStatus.EXITED) {
            throw new IllegalStateException("Session has already exited");
        }
        this.period.setExitTime(exitTime);
        this.amountCharged = amount;
        this.status = ParkingSessionStatus.EXITED;
        registerEvent(new VehicleExited(this, this.sectorCode, exitTime.toLocalDate(), amount));
    }

    @Override
    public boolean sameIdentityAs(ParkingSession other) {
        return other != null && this.id != null && this.id.sameValueAs(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkingSession that = (ParkingSession) o;
        return sameIdentityAs(that);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

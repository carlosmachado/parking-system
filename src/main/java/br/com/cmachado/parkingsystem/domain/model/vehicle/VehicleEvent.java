package br.com.cmachado.parkingsystem.domain.model.vehicle;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import br.com.cmachado.parkingsystem.domain.shared.AggregateRootBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Aggregate root tracking a single vehicle's stay, from entry through parking to exit.
 *
 * <p>State is advanced only through the {@link #enter}, {@link #park} and {@link #exit}
 * methods, which guard the {@link VehicleEventStatus} transitions. On exit the aggregate
 * registers a {@link VehicleExitedDomainEvent} so daily revenue is updated asynchronously.</p>
 */
@Entity
@Table(name = "vehicle_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleEvent extends AggregateRootBase<VehicleEvent> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private LicensePlate licensePlate;

    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "sector_code"))
    private SectorCode sectorCode;

    @Column(name = "spot_id")
    private Long spotId;

    @Embedded
    private Period period;

    @Column(name = "parked_time")
    private LocalDateTime parkedTime;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount_charged"))
    private Money amountCharged;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VehicleEventStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private VehicleEvent(LicensePlate licensePlate, LocalDateTime entryTime) {
        this.licensePlate = licensePlate;
        this.period = new Period(entryTime);
        this.status = VehicleEventStatus.ENTERED;
    }

    /**
     * Creates a new vehicle event in {@code ENTERED} status.
     */
    public static VehicleEvent enter(LicensePlate plate, LocalDateTime entryTime) {
        return new VehicleEvent(plate, entryTime);
    }

    /**
     * Assigns a spot and moves the vehicle to {@code PARKED}.
     *
     * @throws IllegalStateException if the vehicle is not in {@code ENTERED} status
     */
    public void park(Long spotId, SectorCode sectorCode, LocalDateTime parkedTime) {
        if (this.status != VehicleEventStatus.ENTERED) {
            throw new IllegalStateException("Vehicle must be in ENTERED status to park");
        }
        this.spotId = spotId;
        this.sectorCode = sectorCode;
        this.parkedTime = parkedTime;
        this.status = VehicleEventStatus.PARKED;
    }

    /**
     * Records the exit time and final charge, moves the vehicle to {@code EXITED} and
     * registers a {@link VehicleExitedDomainEvent}.
     *
     * @throws IllegalStateException if the vehicle has already exited
     */
    public void exit(LocalDateTime exitTime, Money amount) {
        if (this.status == VehicleEventStatus.EXITED) {
            throw new IllegalStateException("Vehicle has already exited");
        }
        
        this.period.setExitTime(exitTime);
        this.amountCharged = amount;
        this.status = VehicleEventStatus.EXITED;
        
        // Register domain event for async daily revenue update
        registerEvent(new VehicleExitedDomainEvent(this, this.sectorCode, exitTime.toLocalDate(), amount));
    }

    @Override
    public boolean sameIdentityAs(VehicleEvent other) {
        return other != null && this.id != null && this.id.equals(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleEvent that = (VehicleEvent) o;
        return sameIdentityAs(that);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

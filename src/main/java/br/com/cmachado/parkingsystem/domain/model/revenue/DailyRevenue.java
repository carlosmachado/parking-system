package br.com.cmachado.parkingsystem.domain.model.revenue;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.revenue.events.DailyRevenueInitiated;
import br.com.cmachado.parkingsystem.domain.model.revenue.events.DailyRevenueUpdated;
import br.com.cmachado.parkingsystem.domain.shared.AggregateRootBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aggregate root holding the accumulated revenue for one sector on one day.
 *
 * <p>Updated incrementally through {@link #addRevenue(Money)} as vehicles exit, and read
 * back by the {@code GET /revenue} endpoint. The (sector, date) pair is unique.</p>
 */
@Entity
@Table(name = "daily_revenue",
        uniqueConstraints = @UniqueConstraint(name = "uq_daily_revenue_sector_date", columnNames = {"sector_code", "date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyRevenue extends AggregateRootBase<DailyRevenue> {

    @EmbeddedId
    private DailyRevenueId id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @NotNull(message = "Sector code is required")
    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "sector_code", nullable = false))
    private SectorCode sectorCode;

    @NotNull(message = "Date is required")
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @NotNull(message = "Total amount is required")
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_amount", nullable = false))
    private Money totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private DailyRevenue(SectorCode sectorCode, LocalDate date) {
        Objects.requireNonNull(sectorCode, "SectorCode cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");

        this.id = DailyRevenueId.generate();
        this.sectorCode = sectorCode;
        this.date = date;
        this.totalAmount = Money.ZERO;
        registerEvent(new DailyRevenueInitiated(this));
    }

    public static DailyRevenue initiate(SectorCode sectorCode, LocalDate date){
        return new DailyRevenue(sectorCode, date);
    }

    /** Adds the given amount to the running daily total. Null amounts are ignored. */
    public void addRevenue(Money amount) {
        if (amount != null) {
            this.totalAmount = this.totalAmount.add(amount);
            registerEvent(new DailyRevenueUpdated(this));
        }
    }

    @Override
    public boolean sameIdentityAs(DailyRevenue other) {
        return other != null && this.id != null && this.id.sameValueAs(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyRevenue that = (DailyRevenue) o;
        return sameIdentityAs(that);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

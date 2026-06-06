package br.com.cmachado.parkingsystem.fixtures;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;

import java.time.LocalTime;

/** Fluent builder for {@link Sector} aggregates in tests. */
public final class SectorFixture {

    private String code = "A";
    private String basePrice = "10.00";
    private int capacity = 10;
    private LocalTime openHour = LocalTime.MIDNIGHT;
    private LocalTime closeHour = LocalTime.of(23, 59);
    private int durationLimitMinutes = 1440;

    private SectorFixture() {
    }

    public static SectorFixture aSector() {
        return new SectorFixture();
    }

    public SectorFixture withCode(String code) {
        this.code = code;
        return this;
    }

    public SectorFixture withBasePrice(String basePrice) {
        this.basePrice = basePrice;
        return this;
    }

    public SectorFixture withCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public SectorFixture withHours(LocalTime openHour, LocalTime closeHour) {
        this.openHour = openHour;
        this.closeHour = closeHour;
        return this;
    }

    public SectorFixture openAllDay() {
        return withHours(LocalTime.MIDNIGHT, LocalTime.of(23, 59));
    }

    public SectorFixture withDurationLimitMinutes(int durationLimitMinutes) {
        this.durationLimitMinutes = durationLimitMinutes;
        return this;
    }

    public Sector build() {
        return Sector.register(SectorCode.of(code), Money.of(basePrice), capacity,
                openHour, closeHour, durationLimitMinutes);
    }
}

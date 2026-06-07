package br.com.cmachado.parkingsystem.application.revenue.daily;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleExited;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyType;
import br.com.cmachado.parkingsystem.fixtures.ParkingSessionFixture;
import br.com.cmachado.parkingsystem.fixtures.ParkingSpotFixture;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DailyRevenueAsyncListenerTest {

    private static final LocalDateTime EXIT = LocalDateTime.parse("2025-01-01T12:00:00");

    @Mock
    private DailyRevenueUpdater revenueUpdater;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void retriesTransientFailureAndDoesNotIncrementFailureCounterWhenRetrySucceeds() {
        DailyRevenueAsyncListener listener = new DailyRevenueAsyncListener(revenueUpdater, meterRegistry, 3, 0);
        RuntimeException transientFailure = new RuntimeException("deadlock");
        doThrow(transientFailure).doNothing()
                .when(revenueUpdater).addRevenue(eq(SectorCode.of("A")), eq(LocalDate.parse("2025-01-01")), eq(Money.of("10.00")));

        listener.handleVehicleExited(new VehicleExited(exitedSession("RET001", Money.of("10.00"))));

        verify(revenueUpdater, org.mockito.Mockito.times(2))
                .addRevenue(SectorCode.of("A"), LocalDate.parse("2025-01-01"), Money.of("10.00"));
        assertEquals(0.0, meterRegistry.counter("revenue.update.failed").count());
    }

    @Test
    void incrementsFailureCounterOnlyAfterAllAttemptsFail() {
        DailyRevenueAsyncListener listener = new DailyRevenueAsyncListener(revenueUpdater, meterRegistry, 3, 0);
        doThrow(new RuntimeException("db down"))
                .when(revenueUpdater).addRevenue(eq(SectorCode.of("A")), eq(LocalDate.parse("2025-01-01")), eq(Money.of("10.00")));

        listener.handleVehicleExited(new VehicleExited(exitedSession("RET002", Money.of("10.00"))));

        verify(revenueUpdater, org.mockito.Mockito.times(3))
                .addRevenue(SectorCode.of("A"), LocalDate.parse("2025-01-01"), Money.of("10.00"));
        assertEquals(1.0, meterRegistry.counter("revenue.update.failed").count());
    }

    @Test
    void skipsZeroChargeWithoutRetrying() {
        DailyRevenueAsyncListener listener = new DailyRevenueAsyncListener(revenueUpdater, meterRegistry, 3, 0);

        listener.handleVehicleExited(new VehicleExited(exitedSession("RET003", Money.ZERO)));

        verifyNoInteractions(revenueUpdater);
        assertEquals(0.0, meterRegistry.counter("revenue.update.failed").count());
    }

    private ParkingSession exitedSession(String plate, Money amount) {
        ParkingSpot spot = ParkingSpotFixture.aSpot().withSector("A").withLocation(10.0, 10.0).build();
        ParkingSession session = ParkingSessionFixture.aSession()
                .withPlate(plate)
                .enteredAt(EXIT.minusHours(2))
                .parkedOn(spot)
                .build();
        session.exit(EXIT, amount, PricingStrategyType.STANDARD);
        return session;
    }
}

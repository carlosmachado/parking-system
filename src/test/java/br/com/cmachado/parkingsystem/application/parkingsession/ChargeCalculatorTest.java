package br.com.cmachado.parkingsystem.application.parkingsession;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.service.pricing.ChargeCalculator;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.DiscountPricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyFactory;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.StandardPricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.Surcharge10PricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.Surcharge25PricingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeCalculatorTest {

    @Mock private SectorRepository sectorRepository;
    @Mock private ParkingSpotRepository spotRepository;

    private ChargeCalculator calculator;

    private static final LocalDateTime ENTRY = LocalDateTime.parse("2025-01-01T10:00:00");
    private static final LocalDateTime EXIT_2H = LocalDateTime.parse("2025-01-01T12:00:00");
    private static final LocalDateTime EXIT_20M = LocalDateTime.parse("2025-01-01T10:20:00");

    @BeforeEach
    void setUp() {
        PricingStrategyFactory factory = new PricingStrategyFactory(
                new DiscountPricingStrategy(),
                new StandardPricingStrategy(),
                new Surcharge10PricingStrategy(),
                new Surcharge25PricingStrategy());
        calculator = new ChargeCalculator(sectorRepository, spotRepository, factory);
    }

    @Test
    void sectorCodePresentUsesItBasePrice() {
        ParkingSpot parkSpot = spot("A");
        ParkingSession session = parkedSession("CAR001", parkSpot);
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.of(sector("A", "10.00")));
        when(spotRepository.findOccupancyRate()).thenReturn(0.5); // < 0.75 → +10% surcharge

        calculator.charge(session, EXIT_2H);

        // 2h * 10.00 * 1.10 = 22.00
        assertEquals(new BigDecimal("22.00"), session.getAmountCharged().getAmount());
    }

    @Test
    void sectorCodeNullUsesMinBasePrice() {
        ParkingSession session = entered("CAR002");
        when(sectorRepository.findAll()).thenReturn(List.of(
                sector("B", "20.00"),
                sector("A", "5.00")));
        when(spotRepository.findOccupancyRate()).thenReturn(0.0); // < 0.25 → 10% discount

        calculator.charge(session, EXIT_2H);

        // 2h * 5.00 * 0.90 = 9.00
        assertEquals(new BigDecimal("9.00"), session.getAmountCharged().getAmount());
    }

    @Test
    void sectorCodeNotFoundFallsBackToMinBasePrice() {
        ParkingSpot parkSpot = spot("MISSING");
        ParkingSession session = parkedSession("CAR003", parkSpot);
        when(sectorRepository.findByCode(SectorCode.of("MISSING"))).thenReturn(Optional.empty());
        when(sectorRepository.findAll()).thenReturn(List.of(sector("A", "8.00"), sector("B", "12.00")));
        when(spotRepository.findOccupancyRate()).thenReturn(0.3); // < 0.50 → standard

        calculator.charge(session, EXIT_2H);

        // 2h * 8.00 * 1.0 = 16.00
        assertEquals(new BigDecimal("16.00"), session.getAmountCharged().getAmount());
    }

    @Test
    void noSectorsAtAllChargesZero() {
        ParkingSession session = entered("CAR004");
        when(sectorRepository.findAll()).thenReturn(List.of());
        when(spotRepository.findOccupancyRate()).thenReturn(null);

        calculator.charge(session, EXIT_2H);

        assertEquals(BigDecimal.ZERO.setScale(2), session.getAmountCharged().getAmount());
    }

    @Test
    void occupancyRateAboveOneIsCapped() {
        ParkingSpot parkSpot = spot("A");
        ParkingSession session = parkedSession("CAR006", parkSpot);
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.of(sector("A", "10.00")));
        when(spotRepository.findOccupancyRate()).thenReturn(1.5);

        calculator.charge(session, EXIT_2H);

        // 2h * 10.00 * 1.25 = 25.00
        assertEquals(new BigDecimal("25.00"), session.getAmountCharged().getAmount());
    }

    @Test
    void negativeOccupancyRateIsFloored() {
        ParkingSpot parkSpot = spot("A");
        ParkingSession session = parkedSession("CAR007", parkSpot);
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.of(sector("A", "10.00")));
        when(spotRepository.findOccupancyRate()).thenReturn(-0.5);

        calculator.charge(session, EXIT_2H);

        // 2h * 10.00 * 0.90 = 18.00
        assertEquals(new BigDecimal("18.00"), session.getAmountCharged().getAmount());
    }

    @Test
    void withinThirtyMinutesChargesZero() {
        ParkingSpot parkSpot = spot("A");
        ParkingSession session = parkedSession("CAR005", parkSpot);
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.of(sector("A", "10.00")));
        when(spotRepository.findOccupancyRate()).thenReturn(0.5);

        calculator.charge(session, EXIT_20M);

        assertEquals(BigDecimal.ZERO.setScale(2), session.getAmountCharged().getAmount());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ParkingSession entered(String plate) {
        return ParkingSession.enter(LicensePlate.of(plate), ENTRY);
    }

    private ParkingSession parkedSession(String plate, ParkingSpot parkingSpot) {
        ParkingSession session = entered(plate);
        parkingSpot.park(session);
        return session;
    }

    private ParkingSpot spot(String sectorCode) {
        return ParkingSpot.register(1L, SectorCode.of(sectorCode), GeoLocation.of(10.0, 10.0));
    }

    private Sector sector(String code, String basePrice) {
        return Sector.register(SectorCode.of(code), Money.of(basePrice), 10,
                LocalTime.MIDNIGHT, LocalTime.of(23, 59), 1440);
    }
}

package br.com.cmachado.parkingsystem.application.parkingsession;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.service.pricing.ChargeCalculator;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.DiscountPricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyFactory;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.StandardPricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.Surcharge10PricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.Surcharge25PricingStrategy;
import br.com.cmachado.parkingsystem.fixtures.ParkingSessionFixture;
import br.com.cmachado.parkingsystem.fixtures.ParkingSpotFixture;
import br.com.cmachado.parkingsystem.fixtures.SectorFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeCalculatorTest {

    @Mock private SectorRepository sectorRepository;
    @Mock private ParkingSpotRepository spotRepository;

    private ChargeCalculator calculator;

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
    void sectorCodePresentUsesItsBasePrice() {
        // arrange
        ParkingSession session = parkedSessionInSector("A");
        when(sectorRepository.findByCode(SectorCode.of("A")))
                .thenReturn(Optional.of(SectorFixture.aSector().withCode("A").withBasePrice("10.00").build()));
        when(spotRepository.findOccupancyRate()).thenReturn(0.5); // < 0.75 → +10% surcharge

        // act
        calculator.charge(session, EXIT_2H);

        // assert — 2h * 10.00 * 1.10
        assertEquals(new BigDecimal("22.00"), session.getAmountCharged().getAmount(),
                "2h at base 10.00 with 10% surcharge");
    }

    @Test
    void sectorCodeNullUsesMinBasePrice() {
        // arrange — entered (never parked) session has no sector code
        ParkingSession session = ParkingSessionFixture.aSession().withPlate("CAR002").build();
        when(sectorRepository.findMinBasePrice()).thenReturn(Optional.of(new BigDecimal("5.00")));
        when(spotRepository.findOccupancyRate()).thenReturn(0.0); // < 0.25 → 10% discount

        // act
        calculator.charge(session, EXIT_2H);

        // assert — 2h * 5.00 * 0.90
        assertEquals(new BigDecimal("9.00"), session.getAmountCharged().getAmount(),
                "2h at min base 5.00 with 10% discount");
    }

    @Test
    void sectorCodeNotFoundFallsBackToMinBasePrice() {
        // arrange
        ParkingSession session = parkedSessionInSector("MISSING");
        when(sectorRepository.findByCode(SectorCode.of("MISSING"))).thenReturn(Optional.empty());
        when(sectorRepository.findMinBasePrice()).thenReturn(Optional.of(new BigDecimal("8.00")));
        when(spotRepository.findOccupancyRate()).thenReturn(0.3); // < 0.50 → standard

        // act
        calculator.charge(session, EXIT_2H);

        // assert — 2h * 8.00 * 1.0
        assertEquals(new BigDecimal("16.00"), session.getAmountCharged().getAmount(),
                "2h at fallback min base 8.00 standard rate");
    }

    @Test
    void noSectorsAtAllChargesZero() {
        // arrange
        ParkingSession session = ParkingSessionFixture.aSession().withPlate("CAR004").build();
        when(sectorRepository.findMinBasePrice()).thenReturn(Optional.empty());
        when(spotRepository.findOccupancyRate()).thenReturn(null);

        // act
        calculator.charge(session, EXIT_2H);

        // assert
        assertEquals(BigDecimal.ZERO.setScale(2), session.getAmountCharged().getAmount(),
                "no sector data must charge zero");
    }

    @Test
    void occupancyRateAboveOneIsCapped() {
        // arrange
        ParkingSession session = parkedSessionInSector("A");
        when(sectorRepository.findByCode(SectorCode.of("A")))
                .thenReturn(Optional.of(SectorFixture.aSector().withCode("A").withBasePrice("10.00").build()));
        when(spotRepository.findOccupancyRate()).thenReturn(1.5);

        // act
        calculator.charge(session, EXIT_2H);

        // assert — rate capped at 1.0 → 25% surcharge: 2h * 10.00 * 1.25
        assertEquals(new BigDecimal("25.00"), session.getAmountCharged().getAmount(),
                "occupancy above 1.0 must cap at the 25% surcharge tier");
    }

    @Test
    void negativeOccupancyRateIsFloored() {
        // arrange
        ParkingSession session = parkedSessionInSector("A");
        when(sectorRepository.findByCode(SectorCode.of("A")))
                .thenReturn(Optional.of(SectorFixture.aSector().withCode("A").withBasePrice("10.00").build()));
        when(spotRepository.findOccupancyRate()).thenReturn(-0.5);

        // act
        calculator.charge(session, EXIT_2H);

        // assert — rate floored at 0.0 → 10% discount: 2h * 10.00 * 0.90
        assertEquals(new BigDecimal("18.00"), session.getAmountCharged().getAmount(),
                "negative occupancy must floor at the discount tier");
    }

    @Test
    void withinThirtyMinutesChargesZero() {
        // arrange
        ParkingSession session = parkedSessionInSector("A");
        when(sectorRepository.findByCode(SectorCode.of("A")))
                .thenReturn(Optional.of(SectorFixture.aSector().withCode("A").withBasePrice("10.00").build()));
        when(spotRepository.findOccupancyRate()).thenReturn(0.5);

        // act
        calculator.charge(session, EXIT_20M);

        // assert
        assertEquals(BigDecimal.ZERO.setScale(2), session.getAmountCharged().getAmount(),
                "first 30 minutes are free");
    }

    private ParkingSession parkedSessionInSector(String sectorCode) {
        ParkingSpot spot = ParkingSpotFixture.aSpot().withSector(sectorCode).build();
        return ParkingSessionFixture.aSession().parkedOn(spot).build();
    }
}

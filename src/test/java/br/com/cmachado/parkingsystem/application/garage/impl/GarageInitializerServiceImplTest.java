package br.com.cmachado.parkingsystem.application.garage.impl;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.fixtures.GarageResponseFixture;
import br.com.cmachado.parkingsystem.fixtures.SectorFixture;
import br.com.cmachado.parkingsystem.infrastructure.client.GarageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GarageInitializerServiceImplTest {

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    private GarageInitializerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GarageInitializerServiceImpl(sectorRepository, spotRepository);
    }

    @Test
    void nullConfigDoesNothing() {
        // act
        service.initializeGarage(null);

        // assert
        verify(sectorRepository, never()).save(any());
        verify(spotRepository, never()).save(any());
    }

    @Test
    void createsSectorsAndSpotsFromSimulatorConfig() {
        // arrange
        GarageResponse config = GarageResponseFixture.aGarage()
                .withSector(GarageResponseFixture.aSectorData()
                        .withCode("A").withBasePrice(10.0).withMaxCapacity(100)
                        .withHours("08:00", "20:00").withDurationLimitMinutes(240).build())
                .withSpot(GarageResponseFixture.aSpotData()
                        .withId(1L).withSector("A").withLocation(-23.0, -46.0).build())
                .build();
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.empty());
        when(spotRepository.findByExternalId(1L)).thenReturn(Optional.empty());

        // act
        service.initializeGarage(config);

        // assert
        Sector savedSector = capturedSector();
        assertEquals(SectorCode.of("A"), savedSector.getCode(), "sector code");
        assertEquals(Money.of("10.00"), savedSector.getBasePrice(), "base price");
        assertEquals(100, savedSector.getMaxCapacity(), "capacity");
        assertEquals(LocalTime.of(8, 0), savedSector.getOpenHour(), "open hour");
        assertEquals(LocalTime.of(20, 0), savedSector.getCloseHour(), "close hour");
        assertEquals(240, savedSector.getDurationLimitMinutes(), "duration limit");

        ParkingSpot savedSpot = capturedSpot();
        assertEquals(1L, savedSpot.getExternalId(), "external id");
        assertEquals(SectorCode.of("A"), savedSpot.getSectorCode(), "spot sector");
        assertEquals(-23.0, savedSpot.getLocation().getLat(), "spot lat");
        assertEquals(-46.0, savedSpot.getLocation().getLng(), "spot lng");
    }

    @Test
    void updatesExistingSectorWithLatestSimulatorData() {
        // arrange
        Sector existing = SectorFixture.aSector().withCode("A").withBasePrice("8.00").withCapacity(50)
                .withHours(LocalTime.of(7, 0), LocalTime.of(18, 0)).withDurationLimitMinutes(120).build();
        GarageResponse config = GarageResponseFixture.aGarage()
                .withSector(GarageResponseFixture.aSectorData()
                        .withCode("A").withBasePrice(12.5).withMaxCapacity(80)
                        .withHours("09:00", "21:30").withDurationLimitMinutes(300).build())
                .build();
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.of(existing));

        // act
        service.initializeGarage(config);

        // assert
        verify(sectorRepository).save(existing);
        assertEquals(Money.of("12.50"), existing.getBasePrice(), "updated base price");
        assertEquals(80, existing.getMaxCapacity(), "updated capacity");
        assertEquals(LocalTime.of(9, 0), existing.getOpenHour(), "updated open hour");
        assertEquals(LocalTime.of(21, 30), existing.getCloseHour(), "updated close hour");
        assertEquals(300, existing.getDurationLimitMinutes(), "updated duration limit");
    }

    @Test
    void updatesExistingSpotByExternalId() {
        // arrange
        ParkingSpot existing = ParkingSpot.register(7L, SectorCode.of("A"), GeoLocation.of(1.0, 1.0));
        GarageResponse config = GarageResponseFixture.aGarage()
                .withSpot(GarageResponseFixture.aSpotData().withId(7L).withSector("B").withLocation(2.0, 3.0).build())
                .build();
        when(spotRepository.findByExternalId(7L)).thenReturn(Optional.of(existing));

        // act
        service.initializeGarage(config);

        // assert
        verify(spotRepository).save(existing);
        assertEquals(SectorCode.of("B"), existing.getSectorCode(), "updated spot sector");
        assertEquals(2.0, existing.getLocation().getLat(), "updated spot lat");
        assertEquals(3.0, existing.getLocation().getLng(), "updated spot lng");
    }

    @Test
    void blankTimesAndMissingDurationUseDefaults() {
        // arrange
        GarageResponse config = GarageResponseFixture.aGarage()
                .withSector(GarageResponseFixture.aSectorData()
                        .withCode("A").withBasePrice(10.0).withMaxCapacity(10)
                        .withHours(" ", "").withDurationLimitMinutes(null).build())
                .build();
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.empty());

        // act
        service.initializeGarage(config);

        // assert
        Sector saved = capturedSector();
        assertEquals(LocalTime.MIDNIGHT, saved.getOpenHour(), "blank open hour defaults to midnight");
        assertEquals(LocalTime.of(23, 59), saved.getCloseHour(), "blank close hour defaults to 23:59");
        assertEquals(1440, saved.getDurationLimitMinutes(), "missing duration defaults to 1440");
    }

    @Test
    void invalidTimesUseDefaults() {
        // arrange
        GarageResponse config = GarageResponseFixture.aGarage()
                .withSector(GarageResponseFixture.aSectorData()
                        .withCode("A").withBasePrice(10.0).withMaxCapacity(10)
                        .withHours("not-time", "25:99").withDurationLimitMinutes(60).build())
                .build();
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.empty());

        // act
        service.initializeGarage(config);

        // assert
        Sector saved = capturedSector();
        assertEquals(LocalTime.MIDNIGHT, saved.getOpenHour(), "invalid open hour defaults to midnight");
        assertEquals(LocalTime.of(23, 59), saved.getCloseHour(), "invalid close hour defaults to 23:59");
        assertEquals(60, saved.getDurationLimitMinutes(), "valid duration is kept");
    }

    private Sector capturedSector() {
        ArgumentCaptor<Sector> captor = ArgumentCaptor.forClass(Sector.class);
        verify(sectorRepository).save(captor.capture());
        return captor.getValue();
    }

    private ParkingSpot capturedSpot() {
        ArgumentCaptor<ParkingSpot> captor = ArgumentCaptor.forClass(ParkingSpot.class);
        verify(spotRepository).save(captor.capture());
        return captor.getValue();
    }
}

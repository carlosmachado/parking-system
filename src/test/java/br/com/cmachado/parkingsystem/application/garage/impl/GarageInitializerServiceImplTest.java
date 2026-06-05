package br.com.cmachado.parkingsystem.application.garage.impl;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.infrastructure.client.GarageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
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
        service.initializeGarage(null);

        verify(sectorRepository, never()).save(any());
        verify(spotRepository, never()).save(any());
    }

    @Test
    void createsSectorsAndSpotsFromSimulatorConfig() {
        GarageResponse config = config(List.of(sector("A", 10.0, 100, "08:00", "20:00", 240)),
                List.of(spot(1L, "A", -23.0, -46.0)));
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.empty());
        when(spotRepository.findByExternalId(1L)).thenReturn(Optional.empty());

        service.initializeGarage(config);

        ArgumentCaptor<Sector> sectorCaptor = ArgumentCaptor.forClass(Sector.class);
        ArgumentCaptor<ParkingSpot> spotCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        verify(sectorRepository).save(sectorCaptor.capture());
        verify(spotRepository).save(spotCaptor.capture());

        Sector savedSector = sectorCaptor.getValue();
        assertEquals(SectorCode.of("A"), savedSector.getCode());
        assertEquals(Money.of("10.00"), savedSector.getBasePrice());
        assertEquals(100, savedSector.getMaxCapacity());
        assertEquals(LocalTime.of(8, 0), savedSector.getOpenHour());
        assertEquals(LocalTime.of(20, 0), savedSector.getCloseHour());
        assertEquals(240, savedSector.getDurationLimitMinutes());

        ParkingSpot savedSpot = spotCaptor.getValue();
        assertEquals(1L, savedSpot.getExternalId());
        assertEquals(SectorCode.of("A"), savedSpot.getSectorCode());
        assertEquals(-23.0, savedSpot.getLocation().getLat());
        assertEquals(-46.0, savedSpot.getLocation().getLng());
    }

    @Test
    void updatesExistingSectorWithLatestSimulatorData() {
        Sector existing = new Sector(SectorCode.of("A"), Money.of("8.00"), 50,
                LocalTime.of(7, 0), LocalTime.of(18, 0), 120);
        GarageResponse config = config(List.of(sector("A", 12.5, 80, "09:00", "21:30", 300)), null);
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.of(existing));

        service.initializeGarage(config);

        verify(sectorRepository).save(existing);
        assertEquals(Money.of("12.50"), existing.getBasePrice());
        assertEquals(80, existing.getMaxCapacity());
        assertEquals(LocalTime.of(9, 0), existing.getOpenHour());
        assertEquals(LocalTime.of(21, 30), existing.getCloseHour());
        assertEquals(300, existing.getDurationLimitMinutes());
    }

    @Test
    void updatesExistingSpotByExternalId() {
        ParkingSpot existing = ParkingSpot.register(7L, SectorCode.of("A"), GeoLocation.of(1.0, 1.0));
        GarageResponse config = config(null, List.of(spot(7L, "B", 2.0, 3.0)));
        when(spotRepository.findByExternalId(7L)).thenReturn(Optional.of(existing));

        service.initializeGarage(config);

        verify(spotRepository).save(existing);
        assertEquals(SectorCode.of("B"), existing.getSectorCode());
        assertEquals(2.0, existing.getLocation().getLat());
        assertEquals(3.0, existing.getLocation().getLng());
    }

    @Test
    void blankTimesAndMissingDurationUseDefaults() {
        GarageResponse config = config(List.of(sector("A", 10.0, 10, " ", "", null)), null);
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.empty());

        service.initializeGarage(config);

        ArgumentCaptor<Sector> sectorCaptor = ArgumentCaptor.forClass(Sector.class);
        verify(sectorRepository).save(sectorCaptor.capture());
        assertEquals(LocalTime.MIDNIGHT, sectorCaptor.getValue().getOpenHour());
        assertEquals(LocalTime.of(23, 59), sectorCaptor.getValue().getCloseHour());
        assertEquals(1440, sectorCaptor.getValue().getDurationLimitMinutes());
    }

    @Test
    void invalidTimesUseDefaults() {
        GarageResponse config = config(List.of(sector("A", 10.0, 10, "not-time", "25:99", 60)), null);
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.empty());

        service.initializeGarage(config);

        ArgumentCaptor<Sector> sectorCaptor = ArgumentCaptor.forClass(Sector.class);
        verify(sectorRepository).save(sectorCaptor.capture());
        assertEquals(LocalTime.MIDNIGHT, sectorCaptor.getValue().getOpenHour());
        assertEquals(LocalTime.of(23, 59), sectorCaptor.getValue().getCloseHour());
        assertEquals(60, sectorCaptor.getValue().getDurationLimitMinutes());
    }

    private GarageResponse config(List<GarageResponse.SectorData> sectors, List<GarageResponse.SpotData> spots) {
        GarageResponse response = new GarageResponse();
        response.setGarage(sectors);
        response.setSpots(spots);
        return response;
    }

    private GarageResponse.SectorData sector(String code, Double basePrice, Integer maxCapacity,
                                             String openHour, String closeHour, Integer durationLimitMinutes) {
        GarageResponse.SectorData sector = new GarageResponse.SectorData();
        sector.setSector(code);
        sector.setBasePrice(basePrice);
        sector.setMaxCapacity(maxCapacity);
        sector.setOpenHour(openHour);
        sector.setCloseHour(closeHour);
        sector.setDurationLimitMinutes(durationLimitMinutes);
        return sector;
    }

    private GarageResponse.SpotData spot(Long id, String sectorCode, Double lat, Double lng) {
        GarageResponse.SpotData spot = new GarageResponse.SpotData();
        spot.setId(id);
        spot.setSector(sectorCode);
        spot.setLat(lat);
        spot.setLng(lng);
        return spot;
    }
}

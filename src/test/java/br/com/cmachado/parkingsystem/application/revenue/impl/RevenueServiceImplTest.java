package br.com.cmachado.parkingsystem.application.revenue.impl;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenue;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue.RevenueResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueServiceImplTest {

    private static final LocalDate DATE = LocalDate.parse("2025-01-01");

    @Mock
    private DailyRevenueRepository dailyRevenueRepository;

    @Test
    void returnsZeroWhenSectorHasNoRevenueForDate() {
        // arrange
        when(dailyRevenueRepository.findBySectorCodeAndDate(SectorCode.of("A"), DATE))
                .thenReturn(Optional.empty());

        // act
        RevenueResponse response = service().getRevenue(DATE, "A");

        // assert
        assertEquals(BigDecimal.ZERO, response.getAmount(), "no revenue row must report zero");
        assertEquals("BRL", response.getCurrency(), "currency");
        assertNotNull(response.getTimestamp(), "timestamp must be set");
    }

    @Test
    void returnsRevenueForSectorAndDate() {
        // arrange
        when(dailyRevenueRepository.findBySectorCodeAndDate(SectorCode.of("A"), DATE))
                .thenReturn(Optional.of(revenue("A", DATE, "42.50")));

        // act
        RevenueResponse response = service().getRevenue(DATE, "A");

        // assert
        assertEquals(new BigDecimal("42.50"), response.getAmount(), "stored amount for the sector/date");
        assertEquals("BRL", response.getCurrency(), "currency");
    }

    @Test
    void aggregatesAllSectorsForDate() {
        // arrange
        when(dailyRevenueRepository.findByDate(DATE))
                .thenReturn(List.of(revenue("A", DATE, "10.00"), revenue("B", DATE, "15.75")));

        // act
        RevenueResponse response = service().getRevenueAllSectors(DATE);

        // assert
        assertEquals(new BigDecimal("25.75"), response.getAmount(), "sum across all sectors");
        assertEquals("BRL", response.getCurrency(), "currency");
    }

    @Test
    void returnsZeroWhenNoSectorsHaveRevenueForDate() {
        // arrange
        when(dailyRevenueRepository.findByDate(DATE)).thenReturn(List.of());

        // act
        RevenueResponse response = service().getRevenueAllSectors(DATE);

        // assert
        assertEquals(BigDecimal.ZERO, response.getAmount(), "no rows must aggregate to zero");
        assertEquals("BRL", response.getCurrency(), "currency");
    }

    @Test
    void convertsSectorParameterToValueObject() {
        // arrange
        when(dailyRevenueRepository.findBySectorCodeAndDate(SectorCode.of("SEC-A"), DATE))
                .thenReturn(Optional.empty());

        // act
        service().getRevenue(DATE, "SEC-A");

        // assert
        ArgumentCaptor<SectorCode> sectorCaptor = ArgumentCaptor.forClass(SectorCode.class);
        verify(dailyRevenueRepository).findBySectorCodeAndDate(sectorCaptor.capture(), eq(DATE));
        assertEquals(SectorCode.of("SEC-A"), sectorCaptor.getValue(), "sector string must map to a SectorCode");
    }

    private RevenueServiceImpl service() {
        return new RevenueServiceImpl(dailyRevenueRepository);
    }

    private DailyRevenue revenue(String sector, LocalDate date, String amount) {
        DailyRevenue dailyRevenue = DailyRevenue.initiate(SectorCode.of(sector), date);
        dailyRevenue.addRevenue(Money.of(amount));
        return dailyRevenue;
    }
}

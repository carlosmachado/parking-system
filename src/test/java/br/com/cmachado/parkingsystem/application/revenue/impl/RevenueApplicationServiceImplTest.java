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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueApplicationServiceImplTest {

    @Mock
    private DailyRevenueRepository dailyRevenueRepository;

    @Test
    void returnsZeroWhenSectorHasNoRevenueForDate() {
        LocalDate date = LocalDate.parse("2025-01-01");
        when(dailyRevenueRepository.findBySectorCodeAndDate(SectorCode.of("A"), date))
                .thenReturn(Optional.empty());

        RevenueResponse response = service().getRevenue(date, "A");

        assertEquals(BigDecimal.ZERO, response.getAmount());
        assertEquals("BRL", response.getCurrency());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void returnsRevenueForSectorAndDate() {
        LocalDate date = LocalDate.parse("2025-01-01");
        DailyRevenue revenue = revenue("A", date, "42.50");
        when(dailyRevenueRepository.findBySectorCodeAndDate(SectorCode.of("A"), date))
                .thenReturn(Optional.of(revenue));

        RevenueResponse response = service().getRevenue(date, "A");

        assertEquals(new BigDecimal("42.50"), response.getAmount());
        assertEquals("BRL", response.getCurrency());
    }

    @Test
    void aggregatesAllSectorsForDate() {
        LocalDate date = LocalDate.parse("2025-01-01");
        when(dailyRevenueRepository.findByDate(date))
                .thenReturn(List.of(revenue("A", date, "10.00"), revenue("B", date, "15.75")));

        RevenueResponse response = service().getRevenueAllSectors(date);

        assertEquals(new BigDecimal("25.75"), response.getAmount());
        assertEquals("BRL", response.getCurrency());
    }

    @Test
    void returnsZeroWhenNoSectorsHaveRevenueForDate() {
        LocalDate date = LocalDate.parse("2025-01-01");
        when(dailyRevenueRepository.findByDate(date)).thenReturn(List.of());

        RevenueResponse response = service().getRevenueAllSectors(date);

        assertEquals(BigDecimal.ZERO, response.getAmount());
        assertEquals("BRL", response.getCurrency());
    }

    @Test
    void convertsSectorParameterToValueObject() {
        LocalDate date = LocalDate.parse("2025-01-01");
        when(dailyRevenueRepository.findBySectorCodeAndDate(SectorCode.of("SEC-A"), date))
                .thenReturn(Optional.empty());

        service().getRevenue(date, "SEC-A");

        ArgumentCaptor<SectorCode> sectorCaptor = ArgumentCaptor.forClass(SectorCode.class);
        verify(dailyRevenueRepository).findBySectorCodeAndDate(sectorCaptor.capture(), org.mockito.ArgumentMatchers.eq(date));
        assertEquals(SectorCode.of("SEC-A"), sectorCaptor.getValue());
    }

    private RevenueApplicationServiceImpl service() {
        return new RevenueApplicationServiceImpl(dailyRevenueRepository);
    }

    private DailyRevenue revenue(String sector, LocalDate date, String amount) {
        DailyRevenue dailyRevenue = new DailyRevenue(SectorCode.of(sector), date);
        dailyRevenue.addRevenue(Money.of(amount));
        return dailyRevenue;
    }
}

package br.com.cmachado.parkingsystem.application.revenue.impl;

import br.com.cmachado.parkingsystem.application.revenue.RevenueApplicationService;
import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenue;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue.RevenueResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read side for revenue queries: looks up the pre-aggregated {@link DailyRevenue} records
 * (maintained on vehicle exit) for a single sector or all sectors on a given day.
 */
@Service
public class RevenueApplicationServiceImpl implements RevenueApplicationService {

    private final DailyRevenueRepository dailyRevenueRepository;

    public RevenueApplicationServiceImpl(DailyRevenueRepository dailyRevenueRepository) {
        this.dailyRevenueRepository = dailyRevenueRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueResponse getRevenue(LocalDate date, String sectorCode) {
        DailyRevenue dailyRevenue = dailyRevenueRepository
                .findBySectorCodeAndDate(new SectorCode(sectorCode), date)
                .orElse(null);

        BigDecimal amount = dailyRevenue != null && dailyRevenue.getTotalAmount() != null
                ? dailyRevenue.getTotalAmount().getAmount()
                : BigDecimal.ZERO;

        return RevenueResponse.builder()
                .amount(amount)
                .currency("BRL")
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueResponse getRevenueAllSectors(LocalDate date) {
        List<DailyRevenue> revenues = dailyRevenueRepository.findByDate(date);

        BigDecimal total = revenues.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount().getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RevenueResponse.builder()
                .amount(total)
                .currency("BRL")
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}

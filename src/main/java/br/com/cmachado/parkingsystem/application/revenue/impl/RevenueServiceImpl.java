package br.com.cmachado.parkingsystem.application.revenue.impl;

import br.com.cmachado.parkingsystem.application.revenue.RevenueApplicationService;
import br.com.cmachado.parkingsystem.domain.model.common.money.Currency;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenue;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue.RevenueResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
        var sector = SectorCode.of(sectorCode);

        var dailyRevenue = dailyRevenueRepository
                .findBySectorCodeAndDate(sector, date);

        if (dailyRevenue.isEmpty())
            return amountOfZero();

        return amountOf(dailyRevenue.get().getTotalAmount().getAmount());
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueResponse getRevenueAllSectors(LocalDate date) {

        var revenues = dailyRevenueRepository.findByDate(date);

        if (revenues.isEmpty())
            return amountOfZero();

        var amount = revenues.stream()
                .map(x -> x.getTotalAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return amountOf(amount);
    }

    private RevenueResponse amountOf(BigDecimal amount) {
        return RevenueResponse.builder()
                .amount(amount)
                .currency(Currency.BRL.getValue())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    private RevenueResponse amountOfZero() {
        return amountOf(BigDecimal.ZERO);
    }
}

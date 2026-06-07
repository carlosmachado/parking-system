package br.com.cmachado.parkingsystem.application.revenue.impl;

import br.com.cmachado.parkingsystem.application.revenue.RevenueService;
import br.com.cmachado.parkingsystem.domain.model.common.money.Currency;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenue;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue.RevenueResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class RevenueServiceImpl implements RevenueService {

    private static final Logger logger = LoggerFactory.getLogger(RevenueServiceImpl.class);

    private final DailyRevenueRepository dailyRevenueRepository;

    public RevenueServiceImpl(DailyRevenueRepository dailyRevenueRepository) {
        this.dailyRevenueRepository = dailyRevenueRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueResponse getRevenue(LocalDate date, String sectorCode) {
        var sector = SectorCode.of(sectorCode);

        var dailyRevenue = dailyRevenueRepository
                .findBySectorCodeAndDate(sector, date);

        logger.debug("Revenue query: sector={} date={} found={}", sectorCode, date, dailyRevenue.isPresent());

        return dailyRevenue
                .map(revenue -> amountOf(revenue.getTotalAmount().getAmount()))
                .orElseGet(this::amountOfZero);
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueResponse getRevenueAllSectors(LocalDate date) {

        var revenues = dailyRevenueRepository.findByDate(date);

        logger.debug("Revenue query (all sectors): date={} rows={}", date, revenues.size());

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

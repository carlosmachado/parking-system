package br.com.cmachado.parkingsystem.application.revenue;

import br.com.cmachado.parkingsystem.domain.shared.ApplicationService;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue.RevenueResponse;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@ApplicationService
public interface RevenueService {
    @Transactional(readOnly = true)
    RevenueResponse getRevenue(LocalDate date, String sectorCode);

    @Transactional(readOnly = true)
    RevenueResponse getRevenueAllSectors(LocalDate date);
}

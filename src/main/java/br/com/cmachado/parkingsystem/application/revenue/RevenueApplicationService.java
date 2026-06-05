package br.com.cmachado.parkingsystem.application.revenue;

import br.com.cmachado.parkingsystem.domain.shared.ApplicationService;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue.RevenueResponse;

import java.time.LocalDate;

@ApplicationService
public interface RevenueApplicationService {
    RevenueResponse getRevenue(LocalDate date, String sectorCode);
    RevenueResponse getRevenueAllSectors(LocalDate date);
}

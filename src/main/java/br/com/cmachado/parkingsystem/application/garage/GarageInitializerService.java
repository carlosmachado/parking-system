package br.com.cmachado.parkingsystem.application.garage;

import br.com.cmachado.parkingsystem.domain.shared.ApplicationService;
import br.com.cmachado.parkingsystem.infrastructure.client.GarageResponse;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public interface GarageInitializerService {
    @Transactional
    void initializeGarage(GarageResponse config);
}

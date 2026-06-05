package br.com.cmachado.parkingsystem.domain.model.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleEventRepository extends JpaRepository<VehicleEvent, Long> {
    Optional<VehicleEvent> findByLicensePlateAndStatusIn(LicensePlate licensePlate, java.util.List<VehicleEventStatus> statuses);
}

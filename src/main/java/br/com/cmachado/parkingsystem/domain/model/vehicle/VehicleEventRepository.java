package br.com.cmachado.parkingsystem.domain.model.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleEventRepository extends JpaRepository<VehicleEvent, VehicleEventId> {
    Optional<VehicleEvent> findByLicensePlateAndStatusIn(LicensePlate licensePlate, List<VehicleEventStatus> statuses);
}

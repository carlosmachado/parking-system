package br.com.cmachado.parkingsystem.domain.model.parkingsession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, ParkingSessionId> {
    Optional<ParkingSession> findByLicensePlateAndStatusIn(LicensePlate licensePlate, List<ParkingSessionStatus> statuses);
}

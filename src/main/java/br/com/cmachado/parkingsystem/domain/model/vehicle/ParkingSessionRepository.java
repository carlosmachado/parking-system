package br.com.cmachado.parkingsystem.domain.model.vehicle;

import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpotId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, ParkingSessionId> {
    Optional<ParkingSession> findByLicensePlateAndStatusIn(LicensePlate licensePlate, List<ParkingSessionStatus> statuses);
    boolean existsBySpotIdAndStatusAndIdNot(ParkingSpotId spotId, ParkingSessionStatus status, ParkingSessionId excludeId);
}

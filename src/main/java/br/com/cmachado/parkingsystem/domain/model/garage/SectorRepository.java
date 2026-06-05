package br.com.cmachado.parkingsystem.domain.model.garage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, SectorId> {
    Optional<Sector> findByCode(SectorCode code);
}

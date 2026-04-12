package com.eam.proyecto.persistenceLayer.repository;

import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoEntity;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoPasoEntity;
import com.eam.proyecto.persistenceLayer.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlujoTrabajoPasoRepository extends JpaRepository<FlujoTrabajoPasoEntity, Long> {

    List<FlujoTrabajoPasoEntity> findByFlujoTrabajoOrderByOrdenPasoAsc(FlujoTrabajoEntity flujoTrabajo);

    Optional<FlujoTrabajoPasoEntity> findFirstByFlujoTrabajoOrderByOrdenPasoAsc(FlujoTrabajoEntity flujoTrabajo);

    Optional<FlujoTrabajoPasoEntity> findByFlujoTrabajoAndOrdenPaso(
            FlujoTrabajoEntity flujoTrabajo, Integer ordenPaso);

    Optional<FlujoTrabajoPasoEntity> findFirstByFlujoTrabajoAndOrdenPasoGreaterThanOrderByOrdenPasoAsc(
            FlujoTrabajoEntity flujoTrabajo, Integer ordenActual);

    List<FlujoTrabajoPasoEntity> findByRolRequerido(RolEntity rolRequerido);

    Long countByFlujoTrabajo(FlujoTrabajoEntity flujoTrabajo);

    boolean existsByFlujoTrabajoAndOrdenPaso(FlujoTrabajoEntity flujoTrabajo, Integer ordenPaso);
}
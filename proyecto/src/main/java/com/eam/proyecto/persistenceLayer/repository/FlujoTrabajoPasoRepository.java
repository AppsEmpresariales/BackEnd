package com.docucloud.persistence.repository;

import com.docucloud.persistence.entity.FlujoTrabajoEntity;
import com.docucloud.persistence.entity.FlujoTrabajoPasoEntity;
import com.docucloud.persistence.entity.RolEntity;
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
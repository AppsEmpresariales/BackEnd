package com.eam.proyecto.persistenceLayer.repository;

import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoEntity;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.TipoDocumentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlujoTrabajoRepository extends JpaRepository<FlujoTrabajoEntity, Long> {

    List<FlujoTrabajoEntity> findByOrganizacionAndActivoTrue(OrganizacionEntity organizacion);

    List<FlujoTrabajoEntity> findByOrganizacion(OrganizacionEntity organizacion);

    Optional<FlujoTrabajoEntity> findByOrganizacionAndTipoDocumentoAndActivoTrue(
            OrganizacionEntity organizacion, TipoDocumentoEntity tipoDocumento);

    List<FlujoTrabajoEntity> findByOrganizacionAndTipoDocumento(
            OrganizacionEntity organizacion, TipoDocumentoEntity tipoDocumento);

    Optional<FlujoTrabajoEntity> findByIdAndOrganizacion(Long id, OrganizacionEntity organizacion);

    boolean existsByOrganizacionAndTipoDocumentoAndActivoTrue(
            OrganizacionEntity organizacion, TipoDocumentoEntity tipoDocumento);
}
package com.docucloud.persistence.repository;

import com.docucloud.persistence.entity.FlujoTrabajoEntity;
import com.docucloud.persistence.entity.OrganizacionEntity;
import com.docucloud.persistence.entity.TipoDocumentoEntity;
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
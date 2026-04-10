package com.eam.proyecto.persistenceLayer.repository;

import com.eam.proyecto.persistenceLayer.entity.EstadoDocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoDocumentoRepository extends JpaRepository<EstadoDocumentoEntity, Long> {

    List<EstadoDocumentoEntity> findByOrganizacion(OrganizacionEntity organizacion);

    Optional<EstadoDocumentoEntity> findByOrganizacionAndEsInicialTrue(OrganizacionEntity organizacion);

    List<EstadoDocumentoEntity> findByOrganizacionAndEsFinalTrue(OrganizacionEntity organizacion);

    Optional<EstadoDocumentoEntity> findByIdAndEsFinalTrue(Long id);

    Optional<EstadoDocumentoEntity> findByNombreAndOrganizacion(String nombre, OrganizacionEntity organizacion);

    Optional<EstadoDocumentoEntity> findByIdAndOrganizacion(Long id, OrganizacionEntity organizacion);

    List<EstadoDocumentoEntity> findByOrganizacionAndEsFinalFalse(OrganizacionEntity organizacion);
}
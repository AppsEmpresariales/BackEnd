package com.eam.proyecto.persistenceLayer.repository;

import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.TipoDocumentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoDocumentoRepository extends JpaRepository<TipoDocumentoEntity, Long> {

    List<TipoDocumentoEntity> findByOrganizacionAndActiveTrue(OrganizacionEntity organizacion);

    List<TipoDocumentoEntity> findByOrganizacion(OrganizacionEntity organizacion);

    Optional<TipoDocumentoEntity> findByNombreAndOrganizacion(String nombre, OrganizacionEntity organizacion);

    boolean existsByNombreAndOrganizacion(String nombre, OrganizacionEntity organizacion);

    Optional<TipoDocumentoEntity> findByIdAndOrganizacion(Long id, OrganizacionEntity organizacion);

    List<TipoDocumentoEntity> findByOrganizacionAndNombreContainingIgnoreCase(
            OrganizacionEntity organizacion, String nombre);

    Long countByOrganizacionAndActiveTrue(OrganizacionEntity organizacion);
}
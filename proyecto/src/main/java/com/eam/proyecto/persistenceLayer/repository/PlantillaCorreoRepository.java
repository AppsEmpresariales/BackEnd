package com.eam.proyecto.persistenceLayer.repository;

import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.PlantillaCorreoEntity;
import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantillaCorreoRepository extends JpaRepository<PlantillaCorreoEntity, Long> {

    Optional<PlantillaCorreoEntity> findByOrganizacionAndTipoEventoAndActivoTrue(
            OrganizacionEntity organizacion, TipoEventoEnum tipoEvento);

    List<PlantillaCorreoEntity> findByOrganizacionAndActivoTrue(OrganizacionEntity organizacion);

    List<PlantillaCorreoEntity> findByOrganizacion(OrganizacionEntity organizacion);

    Optional<PlantillaCorreoEntity> findByIdAndOrganizacion(Long id, OrganizacionEntity organizacion);

    boolean existsByOrganizacionAndTipoEventoAndActivoTrue(
            OrganizacionEntity organizacion, TipoEventoEnum tipoEvento);

    List<PlantillaCorreoEntity> findByOrganizacionAndTipoEvento(
            OrganizacionEntity organizacion, TipoEventoEnum tipoEvento);
}
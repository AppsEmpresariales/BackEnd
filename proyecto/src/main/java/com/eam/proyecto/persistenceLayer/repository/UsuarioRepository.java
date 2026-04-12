package com.eam.proyecto.persistenceLayer.repository;

import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UsuarioEntity> findByCedula(Long cedula);

    List<UsuarioEntity> findByOrganizacion(OrganizacionEntity organizacion);

    List<UsuarioEntity> findByOrganizacionAndActiveTrue(OrganizacionEntity organizacion);

    List<UsuarioEntity> findByOrganizacionAndActiveFalse(OrganizacionEntity organizacion);

    Optional<UsuarioEntity> findByEmailAndOrganizacion(String email, OrganizacionEntity organizacion);

    Optional<UsuarioEntity> findByEmailAndActiveTrue(String email);

    List<UsuarioEntity> findByOrganizacionAndNombreContainingIgnoreCase(
            OrganizacionEntity organizacion, String nombre);

    Long countByOrganizacionAndActiveTrue(OrganizacionEntity organizacion);

    boolean existsByCedulaAndOrganizacion(Long cedula, OrganizacionEntity organizacion);

    List<UsuarioEntity> findByOrganizacionOrderByNombreAsc(OrganizacionEntity organizacion);

    @Query("SELECT u FROM UsuarioEntity u WHERE u.organizacion = :org AND u.email = :email AND u.cedula <> :cedula")
    Optional<UsuarioEntity> findByOrganizacionAndEmailExcluyendo(
            @Param("org") OrganizacionEntity organizacion,
            @Param("email") String email,
            @Param("cedula") Long cedula);
}
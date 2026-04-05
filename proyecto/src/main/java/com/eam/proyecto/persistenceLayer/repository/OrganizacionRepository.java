package com.docucloud.persistence.repository;

import com.docucloud.persistence.entity.OrganizacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizacionRepository extends JpaRepository<OrganizacionEntity, Long> {

    Optional<OrganizacionEntity> findByNit(Long nit);

    boolean existsByNit(Long nit);

    Optional<OrganizacionEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<OrganizacionEntity> findByActiveTrue();

    List<OrganizacionEntity> findByActiveFalse();

    List<OrganizacionEntity> findByNombreContainingIgnoreCase(String nombre);

    Optional<OrganizacionEntity> findByNitAndActiveTrue(Long nit);

    Long countByActiveTrue();
}
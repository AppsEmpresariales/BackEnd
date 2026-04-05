package com.docucloud.persistence.repository;

import com.docucloud.persistence.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<RolEntity, Long> {

    Optional<RolEntity> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
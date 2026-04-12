package com.eam.proyecto.persistenceLayer.repository;

import com.eam.proyecto.persistenceLayer.entity.RolEntity;
import com.eam.proyecto.persistenceLayer.entity.RolUsuarioEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolUsuarioRepository extends JpaRepository<RolUsuarioEntity, Long> {

    List<RolUsuarioEntity> findByUsuario(UsuarioEntity usuario);

    Optional<RolUsuarioEntity> findByUsuarioAndRol(UsuarioEntity usuario, RolEntity rol);

    boolean existsByUsuarioAndRol(UsuarioEntity usuario, RolEntity rol);

    void deleteByUsuarioAndRol(UsuarioEntity usuario, RolEntity rol);

    List<RolUsuarioEntity> findByRol(RolEntity rol);

    @Query("SELECT ru FROM RolUsuarioEntity ru WHERE ru.usuario.cedula = :cedula")
    List<RolUsuarioEntity> findByUsuarioCedula(@Param("cedula") Long cedula);
}
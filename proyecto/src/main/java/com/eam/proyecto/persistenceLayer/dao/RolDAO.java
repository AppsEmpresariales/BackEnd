package com.eam.proyecto.persistenceLayer.dao;

import com.eam.proyecto.businessLayer.dto.RolDTO;
import com.eam.proyecto.persistenceLayer.mapper.RolMapper;
import com.eam.proyecto.persistenceLayer.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de roles.
 *
 * DESCRIPCION:
 * - RolEntity es un catálogo de datos maestros: ADMIN_ORG y USER_ESTANDAR.
 * - No se crean ni eliminan roles desde la aplicación (son datos maestros).
 * - Solo se consultan para asignaciones y validaciones de acceso.
 *
 * HISTORIAS CUBIERTAS:
 * - US-005 (RF05): Gestionar roles ADMIN_ORG y USER_ESTANDAR → findAll, findByNombre
 * - US-006 (RF06): Restringir acceso según rol → findByNombre para Spring Security
 * - US-015 (RF15): Asignar rol a usuario → findById, findByNombre
 */
@Repository
@RequiredArgsConstructor
public class RolDAO {

    private final RolRepository rolRepository;
    private final RolMapper rolMapper;

    /**
     * Buscar rol por ID.
     *
     * CASO DE USO: Validar que el rol existe antes de asignarlo a un usuario (US-015).
     */
    public Optional<RolDTO> findById(Long id) {
        return rolRepository.findById(id)
                .map(rolMapper::toDTO);
    }

    /**
     * Listar todos los roles disponibles.
     *
     * US-005: Catálogo de roles para administración.
     */
    public List<RolDTO> findAll() {
        return rolMapper.toDTOList(rolRepository.findAll());
    }

    /**
     * Buscar rol por nombre exacto.
     *
     * CASO DE USO: Spring Security carga el rol del usuario por nombre
     * para evaluar permisos (US-006). También usado para asignaciones (US-015).
     */
    public Optional<RolDTO> findByNombre(String nombre) {
        return rolRepository.findByNombre(nombre)
                .map(rolMapper::toDTO);
    }

    /**
     * Verificar si existe un rol con ese nombre.
     *
     * CASO DE USO: Validación previa antes de intentar asignar un rol.
     */
    public boolean existsByNombre(String nombre) {
        return rolRepository.existsByNombre(nombre);
    }

    /**
     * Contar total de roles registrados.
     *
     * CASO DE USO: Verificación de datos maestros en arranque del sistema.
     */
    public long count() {
        return rolRepository.count();
    }
}

package com.docucloud.persistence.dao;

import com.docucloud.businessLayer.dto.RolUsuarioAsignarDTO;
import com.docucloud.businessLayer.dto.RolUsuarioDTO;
import com.docucloud.businessLayer.mapper.RolUsuarioMapper;
import com.docucloud.persistence.entity.RolEntity;
import com.docucloud.persistence.entity.RolUsuarioEntity;
import com.docucloud.persistence.entity.UsuarioEntity;
import com.docucloud.persistence.repository.RolUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de asignaciones de rol a usuario.
 *
 * DESCRIPCION:
 * - RolUsuarioEntity es una tabla de unión con constraint UNIQUE(user_id, rol_id).
 * - Un usuario puede tener múltiples roles dentro de la misma organización.
 * - El service debe verificar existencia antes de insertar para evitar
 *   DataIntegrityViolationException por el constraint UNIQUE.
 *
 * HISTORIAS CUBIERTAS:
 * - US-005 (RF05): Gestionar roles ADMIN_ORG y USER_ESTANDAR → findByUsuarioCedula
 * - US-006 (RF06): Restringir acceso según rol → findByUsuarioCedula para cargar autoridades
 * - US-015 (RF15): Asignar o cambiar rol → save, deleteByUsuarioCedulaAndRolId
 */
@Repository
@RequiredArgsConstructor
public class RolUsuarioDAO {

    private final RolUsuarioRepository rolUsuarioRepository;
    private final RolUsuarioMapper rolUsuarioMapper;

    /**
     * Asignar un rol a un usuario.
     *
     * FLUJO:
     * 1. AsignarDTO → Entity (mapper construye refs de UsuarioEntity y RolEntity)
     * 2. Guardar Entity → DTO
     *
     * PRECONDICIÓN: El service debe verificar con existeAsignacion() antes de llamar.
     * US-015
     */
    public RolUsuarioDTO save(RolUsuarioAsignarDTO asignarDTO) {
        RolUsuarioEntity entity = rolUsuarioMapper.toEntity(asignarDTO);
        return rolUsuarioMapper.toDTO(rolUsuarioRepository.save(entity));
    }

    /**
     * Listar todos los roles de un usuario por cédula.
     *
     * US-006: Spring Security usa este método para cargar las autoridades del usuario.
     * US-005: Vista de roles asignados al usuario.
     */
    public List<RolUsuarioDTO> findByUsuarioCedula(Long cedula) {
        return rolUsuarioMapper.toDTOList(
                rolUsuarioRepository.findByUsuarioCedula(cedula));
    }

    /**
     * Listar todos los usuarios que tienen un rol específico.
     *
     * CASO DE USO: Ver quiénes son ADMIN_ORG en la organización.
     */
    public List<RolUsuarioDTO> findByRolId(Long rolId) {
        RolEntity rolRef = buildRolRef(rolId);
        return rolUsuarioMapper.toDTOList(rolUsuarioRepository.findByRol(rolRef));
    }

    /**
     * Verificar si un usuario ya tiene un rol específico asignado.
     *
     * CASO DE USO: Evitar duplicados antes de insertar (constraint UNIQUE en BD).
     * US-015
     */
    public boolean existeAsignacion(Long cedula, Long rolId) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        RolEntity rolRef = buildRolRef(rolId);
        return rolUsuarioRepository.existsByUsuarioAndRol(usuarioRef, rolRef);
    }

    /**
     * Buscar la asignación específica de un usuario y un rol.
     *
     * CASO DE USO: Obtener el registro antes de eliminarlo (US-015 cambio de rol).
     */
    public Optional<RolUsuarioDTO> findByUsuarioCedulaAndRolId(Long cedula, Long rolId) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        RolEntity rolRef = buildRolRef(rolId);
        return rolUsuarioRepository.findByUsuarioAndRol(usuarioRef, rolRef)
                .map(rolUsuarioMapper::toDTO);
    }

    /**
     * Eliminar la asignación de un rol a un usuario.
     *
     * US-015: Cambiar rol implica eliminar el rol anterior y asignar el nuevo.
     */
    public void deleteByUsuarioCedulaAndRolId(Long cedula, Long rolId) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        RolEntity rolRef = buildRolRef(rolId);
        rolUsuarioRepository.deleteByUsuarioAndRol(usuarioRef, rolRef);
    }

    /**
     * Contar total de asignaciones de rol.
     *
     * CASO DE USO: Estadísticas administrativas.
     */
    public long count() {
        return rolUsuarioRepository.count();
    }

    // ─── Métodos auxiliares privados ─────────────────────────────────────────

    private UsuarioEntity buildUsuarioRef(Long cedula) {
        UsuarioEntity u = new UsuarioEntity();
        u.setCedula(cedula);
        return u;
    }

    private RolEntity buildRolRef(Long rolId) {
        RolEntity r = new RolEntity();
        r.setId(rolId);
        return r;
    }
}

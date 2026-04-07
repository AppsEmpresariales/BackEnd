package com.docucloud.businessLayer.mapper;

import com.docucloud.businessLayer.dto.RolUsuarioAsignarDTO;
import com.docucloud.businessLayer.dto.RolUsuarioDTO;
import com.docucloud.persistence.entity.RolEntity;
import com.docucloud.persistence.entity.RolUsuarioEntity;
import com.docucloud.persistence.entity.UsuarioEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre RolUsuarioEntity y DTOs usando MapStruct.
 *
 * COMPLEJIDAD ADICIONAL:
 * - RolUsuarioEntity es una tabla de unión con constraint UNIQUE(user_id, rol_id).
 * - Tiene dos relaciones ManyToOne: UsuarioEntity y RolEntity.
 * - RolUsuarioDTO expone información denormalizada de ambas relaciones.
 *
 * HISTORIAS CUBIERTAS:
 * - US-005: Gestionar roles ADMIN_ORG y USER_ESTANDAR (RF05) → asignación de roles.
 * - US-006: Restringir acceso según rol (RF06) → toDTOList para cargar roles del usuario.
 * - US-007: Gestionar usuarios por organización como módulo centralizado (RF07) → vista de roles.
 * - US-015: Asignar o cambiar rol de un usuario (RF15) → toEntity(RolUsuarioAsignarDTO).
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RolUsuarioMapper {

    /**
     * Convierte RolUsuarioEntity a RolUsuarioDTO (LECTURA).
     *
     * MAPEOS PERSONALIZADOS:
     * - usuarioCedula  → usuario.cedula
     * - usuarioNombre  → usuario.nombre
     * - rolId          → rol.id
     * - rolNombre      → rol.nombre
     */
    @Mapping(target = "usuarioCedula", source = "usuario.cedula")
    @Mapping(target = "usuarioNombre", source = "usuario.nombre")
    @Mapping(target = "rolId",         source = "rol.id")
    @Mapping(target = "rolNombre",     source = "rol.nombre")
    RolUsuarioDTO toDTO(RolUsuarioEntity entity);

    /**
     * Convierte lista de RolUsuarioEntity a lista de RolUsuarioDTO.
     */
    List<RolUsuarioDTO> toDTOList(List<RolUsuarioEntity> entities);

    /**
     * Convierte RolUsuarioAsignarDTO a RolUsuarioEntity (ASIGNAR ROL A USUARIO).
     *
     * CAMPOS IGNORADOS:
     * - id: autogenerado.
     *
     * NOTA: Si el usuario ya tiene el rol asignado, el constraint UNIQUE
     * en BD lanzará DataIntegrityViolationException — el service debe
     * verificar la existencia antes de persistir (US-015).
     */
    @Mapping(target = "id",      ignore = true)
    @Mapping(target = "usuario", source = "usuarioCedula", qualifiedByName = "cedulaToUsuarioEntity")
    @Mapping(target = "rol",     source = "rolId",         qualifiedByName = "idToRolEntity")
    RolUsuarioEntity toEntity(RolUsuarioAsignarDTO asignarDTO);

    // ─── Métodos auxiliares ───────────────────────────────────────────────────

    @Named("cedulaToUsuarioEntity")
    default UsuarioEntity cedulaToUsuarioEntity(Long cedula) {
        if (cedula == null) return null;
        UsuarioEntity u = new UsuarioEntity();
        u.setCedula(cedula);
        return u;
    }

    @Named("idToRolEntity")
    default RolEntity idToRolEntity(Long id) {
        if (id == null) return null;
        RolEntity r = new RolEntity();
        r.setId(id);
        return r;
    }
}

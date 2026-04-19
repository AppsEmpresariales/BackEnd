package com.eam.proyecto.persistenceLayer.mapper;

import com.eam.proyecto.businessLayer.dto.UsuarioCreateDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioUpdateDTO;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre UsuarioEntity y DTOs usando MapStruct.
 *
 * COMPLEJIDAD ADICIONAL:
 * - UsuarioEntity tiene relación con OrganizacionEntity (ManyToOne).
 * - UsuarioDTO incluye información denormalizada de la organización (organizacionNit, organizacionNombre).
 * - Necesitamos mapear organizacionNit ↔ OrganizacionEntity.
 *
 * HISTORIAS CUBIERTAS:
 * - US-002: Iniciar sesión (RF02) → lectura de usuario por email.
 * - US-009: Asociar usuarios a su organización (RF09) → toEntity usa organizacionNit.
 * - US-012: Crear usuario dentro de la organización (RF12) → toEntity(UsuarioCreateDTO).
 * - US-013: Editar datos de usuario (RF13) → updateEntityFromDTO(...).
 * - US-014: Activar / inactivar cuenta (RF14) → campo 'active' gestionado en el service.
 * - US-015: Asignar o cambiar rol (RF15) → gestionado en RolUsuarioMapper.
 * - US-016: Listar usuarios de la organización (RF16) → toDTOList(...).
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface UsuarioMapper {

    /**
     * Convierte UsuarioEntity a UsuarioDTO (LECTURA).
     *
     * MAPEOS PERSONALIZADOS:
     * - organizacionNit   → extraído de organizacion.nit
     * - organizacionNombre → extraído de organizacion.nombre
     *
     * CAMPO OMITIDO:
     * - passwordHash: nunca se expone en el DTO de salida.
     */
    @Mapping(target = "organizacionNit", source = "organizacion.nit")
    @Mapping(target = "organizacionNombre", source = "organizacion.nombre")
    UsuarioDTO toDTO(UsuarioEntity entity);

    /**
     * Convierte lista de UsuarioEntity a lista de UsuarioDTO.
     */
    List<UsuarioDTO> toDTOList(List<UsuarioEntity> entities);

    /**
     * Convierte UsuarioCreateDTO a UsuarioEntity (CREAR).
     *
     * MAPEO COMPLEJO:
     * - organizacionNit del DTO se convierte en OrganizacionEntity con solo el NIT.
     *   JPA maneja la relación sin necesidad de cargar el objeto completo.
     *
     * CAMPOS IGNORADOS:
     * - creadoEn / actualizadoEn: los gestiona JPA.
     * - active: se establece en true por defecto en el service.
     */
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "organizacion", source = "organizacionNit", qualifiedByName = "nitToOrganizacionEntity")
    UsuarioEntity toEntity(UsuarioCreateDTO createDTO);

    /**
     * Actualiza UsuarioEntity existente con datos de UsuarioUpdateDTO.
     *
     * NOTAS IMPORTANTES:
     * - NO actualizamos cedula (PK inmutable).
     * - NO actualizamos organizacion (el usuario no cambia de organización).
     * - passwordHash: se actualiza desde el service DESPUÉS de encriptar con BCrypt.
     * - Estrategia IGNORE para null → actualización parcial (US-013).
     */
    @Mapping(target = "cedula", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "actualizadoEn", ignore = true)
    @Mapping(target = "organizacion", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(UsuarioUpdateDTO updateDTO, @MappingTarget UsuarioEntity entity);

    /**
     * MÉTODO AUXILIAR: Crea OrganizacionEntity con solo el NIT.
     *
     * ¿POR QUÉ ESTE MÉTODO?
     * - Al crear un usuario, solo tenemos el organizacionNit del request.
     * - No necesitamos cargar toda la organización desde BD, solo la referencia FK.
     * - JPA maneja la FK correctamente con solo el NIT asignado.
     */
    @Named("nitToOrganizacionEntity")
    default OrganizacionEntity nitToOrganizacionEntity(Long nit) {
        if (nit == null) return null;
        OrganizacionEntity org = new OrganizacionEntity();
        org.setNit(nit);
        return org;
    }
}

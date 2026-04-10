package com.eam.proyecto.persistenceLayer.mapper;

import com.eam.proyecto.businessLayer.dto.PlantillaCorreoCreateDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoUpdateDTO;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.PlantillaCorreoEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre PlantillaCorreoEntity y DTOs usando MapStruct.
 *
 * COMPLEJIDAD ADICIONAL:
 * - PlantillaCorreoEntity contiene el Enum TipoEventoEnum:
 *     DOCUMENTO_CREADO, TAREA_ASIGNADA, TAREA_VENCIDA,
 *     DOCUMENTO_APROBADO, DOCUMENTO_RECHAZADO, NOTIFICACION_GENERAL.
 *   MapStruct mapea el enum automáticamente por nombre.
 * - Tiene relación ManyToOne con OrganizacionEntity.
 *
 * HISTORIAS CUBIERTAS:
 * - US-040: Gestionar plantillas de correo reutilizables (RF40) → CRUD completo.
 * - US-043: Configurar plantillas de correo por evento del sistema (RF43) → campo tipoEvento.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface PlantillaCorreoMapper {

    /**
     * Convierte PlantillaCorreoEntity a PlantillaCorreoDTO (LECTURA).
     *
     * MAPEOS PERSONALIZADOS:
     * - organizacionNit    → organizacion.nit
     * - organizacionNombre → organizacion.nombre
     *
     * ENUM:
     * - tipoEvento: TipoEventoEnum → String, mapeado automáticamente.
     */
    @Mapping(target = "organizacionNit",    source = "organizacion.nit")
    @Mapping(target = "organizacionNombre", source = "organizacion.nombre")
    PlantillaCorreoDTO toDTO(PlantillaCorreoEntity entity);

    /**
     * Convierte lista de PlantillaCorreoEntity a lista de PlantillaCorreoDTO.
     */
    List<PlantillaCorreoDTO> toDTOList(List<PlantillaCorreoEntity> entities);

    /**
     * Convierte PlantillaCorreoCreateDTO a PlantillaCorreoEntity (CREAR).
     *
     * CAMPOS IGNORADOS:
     * - id: autogenerado.
     * - activo: el service lo inicializa en true al crear (US-040).
     */
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "activo",       ignore = true)
    @Mapping(target = "organizacion", source = "organizacionNit", qualifiedByName = "nitToOrganizacionEntity")
    PlantillaCorreoEntity toEntity(PlantillaCorreoCreateDTO createDTO);

    /**
     * Actualiza PlantillaCorreoEntity existente con datos de PlantillaCorreoUpdateDTO.
     *
     * CAMPOS NO ACTUALIZABLES:
     * - id, organizacion: inmutables.
     * - activo: se gestiona mediante endpoint dedicado de activar/desactivar (US-040).
     * - tipoEvento: la plantilla no cambia el evento al que responde (requiere recreación).
     * Estrategia IGNORE para null → actualización parcial.
     */
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "organizacion", ignore = true)
    @Mapping(target = "activo",       ignore = true)
    @Mapping(target = "tipoEvento",   ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(PlantillaCorreoUpdateDTO updateDTO, @MappingTarget PlantillaCorreoEntity entity);

    /**
     * MÉTODO AUXILIAR: Crea OrganizacionEntity con solo el NIT para establecer la FK.
     */
    @Named("nitToOrganizacionEntity")
    default OrganizacionEntity nitToOrganizacionEntity(Long nit) {
        if (nit == null) return null;
        OrganizacionEntity org = new OrganizacionEntity();
        org.setNit(nit);
        return org;
    }
}

package com.eam.proyecto.persistenceLayer.mapper;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoUpdateDTO;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoEntity;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.TipoDocumentoEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre FlujoTrabajoEntity y DTOs usando MapStruct.
 *
 * COMPLEJIDAD ADICIONAL:
 * - FlujoTrabajoEntity tiene relación con OrganizacionEntity y TipoDocumentoEntity.
 * - FlujoTrabajoDTO expone organizacionNit, organizacionNombre, tipoDocumentoId, tipoDocumentoNombre.
 *
 * HISTORIAS CUBIERTAS:
 * - US-028: Definir flujo de aprobación de documentos (RF28) → toEntity(FlujoTrabajoCreateDTO).
 * - US-032: Parametrizar flujos por tipo documental (RF32) → campo tipoDocumentoId.
 * - US-044: Configurar flujos de proceso documentales (RF44) → updateEntityFromDTO(...).
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface FlujoTrabajoMapper {

    /**
     * Convierte FlujoTrabajoEntity a FlujoTrabajoDTO (LECTURA).
     *
     * MAPEOS PERSONALIZADOS:
     * - organizacionNit    → organizacion.nit
     * - organizacionNombre → organizacion.nombre
     * - tipoDocumentoId    → tipoDocumento.id
     * - tipoDocumentoNombre → tipoDocumento.nombre
     */
    @Mapping(target = "organizacionNit",     source = "organizacion.nit")
    @Mapping(target = "organizacionNombre",  source = "organizacion.nombre")
    @Mapping(target = "tipoDocumentoId",     source = "tipoDocumento.id")
    @Mapping(target = "tipoDocumentoNombre", source = "tipoDocumento.nombre")
    FlujoTrabajoDTO toDTO(FlujoTrabajoEntity entity);

    /**
     * Convierte lista de FlujoTrabajoEntity a lista de FlujoTrabajoDTO.
     */
    List<FlujoTrabajoDTO> toDTOList(List<FlujoTrabajoEntity> entities);

    /**
     * Convierte FlujoTrabajoCreateDTO a FlujoTrabajoEntity (CREAR).
     *
     * CAMPOS IGNORADOS:
     * - id: autogenerado.
     */
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "organizacion",  source = "organizacionNit",  qualifiedByName = "nitToOrganizacionEntity")
    @Mapping(target = "tipoDocumento", source = "tipoDocumentoId",  qualifiedByName = "idToTipoDocumentoEntity")
    FlujoTrabajoEntity toEntity(FlujoTrabajoCreateDTO createDTO);

    /**
     * Actualiza FlujoTrabajoEntity existente con datos de FlujoTrabajoUpdateDTO.
     *
     * CAMPOS NO ACTUALIZABLES:
     * - id, organizacion: inmutables.
     * - tipoDocumento: el flujo no cambia de tipo documental (requiere recreación).
     * Estrategia IGNORE para null → actualización parcial (US-044).
     */
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "organizacion",  ignore = true)
    @Mapping(target = "tipoDocumento", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(FlujoTrabajoUpdateDTO updateDTO, @MappingTarget FlujoTrabajoEntity entity);

    // ─── Métodos auxiliares ───────────────────────────────────────────────────

    @Named("nitToOrganizacionEntity")
    default OrganizacionEntity nitToOrganizacionEntity(Long nit) {
        if (nit == null) return null;
        OrganizacionEntity org = new OrganizacionEntity();
        org.setNit(nit);
        return org;
    }

    @Named("idToTipoDocumentoEntity")
    default TipoDocumentoEntity idToTipoDocumentoEntity(Long id) {
        if (id == null) return null;
        TipoDocumentoEntity t = new TipoDocumentoEntity();
        t.setId(id);
        return t;
    }
}

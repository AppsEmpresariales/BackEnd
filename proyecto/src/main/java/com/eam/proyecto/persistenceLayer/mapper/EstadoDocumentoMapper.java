package com.docucloud.businessLayer.mapper;

import com.docucloud.businessLayer.dto.EstadoDocumentoCreateDTO;
import com.docucloud.businessLayer.dto.EstadoDocumentoDTO;
import com.docucloud.businessLayer.dto.EstadoDocumentoUpdateDTO;
import com.docucloud.persistence.entity.EstadoDocumentoEntity;
import com.docucloud.persistence.entity.OrganizacionEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre EstadoDocumentoEntity y DTOs usando MapStruct.
 *
 * NOTA DE DISEÑO:
 * - EstadoDocumento es un catálogo real en BD (NOT un enum de Java).
 * - Cada organización puede tener sus propios estados personalizados.
 * - Los campos esInicial y esFinal controlan el ciclo de vida del flujo documental.
 *
 * HISTORIAS CUBIERTAS:
 * - US-030: Cambiar estado del documento durante el flujo (RF30) → referencia usada en DocumentoMapper.
 * - US-041: Parametrizar estados de documentos en el sistema (RF41) → CRUD completo de estados.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface EstadoDocumentoMapper {

    /**
     * Convierte EstadoDocumentoEntity a EstadoDocumentoDTO (LECTURA).
     *
     * MAPEOS PERSONALIZADOS:
     * - organizacionNit    → organizacion.nit
     * - organizacionNombre → organizacion.nombre
     */
    @Mapping(target = "organizacionNit",    source = "organizacion.nit")
    @Mapping(target = "organizacionNombre", source = "organizacion.nombre")
    EstadoDocumentoDTO toDTO(EstadoDocumentoEntity entity);

    /**
     * Convierte lista de EstadoDocumentoEntity a lista de EstadoDocumentoDTO.
     */
    List<EstadoDocumentoDTO> toDTOList(List<EstadoDocumentoEntity> entities);

    /**
     * Convierte EstadoDocumentoCreateDTO a EstadoDocumentoEntity (CREAR).
     *
     * CAMPOS IGNORADOS:
     * - id: autogenerado.
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "organizacion", source = "organizacionNit", qualifiedByName = "nitToOrganizacionEntity")
    EstadoDocumentoEntity toEntity(EstadoDocumentoCreateDTO createDTO);

    /**
     * Actualiza EstadoDocumentoEntity existente con datos de EstadoDocumentoUpdateDTO.
     *
     * CAMPOS NO ACTUALIZABLES:
     * - id, organizacion: inmutables.
     * Estrategia IGNORE para null → actualización parcial (US-041).
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "organizacion", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(EstadoDocumentoUpdateDTO updateDTO, @MappingTarget EstadoDocumentoEntity entity);

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

package com.docucloud.businessLayer.mapper;

import com.docucloud.businessLayer.dto.TipoDocumentoCreateDTO;
import com.docucloud.businessLayer.dto.TipoDocumentoDTO;
import com.docucloud.businessLayer.dto.TipoDocumentoUpdateDTO;
import com.docucloud.persistence.entity.OrganizacionEntity;
import com.docucloud.persistence.entity.TipoDocumentoEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre TipoDocumentoEntity y DTOs usando MapStruct.
 *
 * HISTORIAS CUBIERTAS:
 * - US-024: Crear tipo de documento personalizado (RF24) → toEntity(TipoDocumentoCreateDTO).
 * - US-025: Editar tipo documental existente (RF25) → updateEntityFromDTO(...).
 * - US-026: Eliminar (desactivar) tipo documental (RF26) → campo 'active' gestionado en el service.
 * - US-027: Asociar documentos a un tipo documental (RF27) → referencia usada en DocumentoMapper.
 * - US-042: Parametrizar tipos documentales desde configuración (RF42) → mismos métodos.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface TipoDocumentoMapper {

    /**
     * Convierte TipoDocumentoEntity a TipoDocumentoDTO (LECTURA).
     *
     * MAPEOS PERSONALIZADOS:
     * - organizacionNit    → organizacion.nit
     * - organizacionNombre → organizacion.nombre
     */
    @Mapping(target = "organizacionNit",    source = "organizacion.nit")
    @Mapping(target = "organizacionNombre", source = "organizacion.nombre")
    TipoDocumentoDTO toDTO(TipoDocumentoEntity entity);

    /**
     * Convierte lista de TipoDocumentoEntity a lista de TipoDocumentoDTO.
     */
    List<TipoDocumentoDTO> toDTOList(List<TipoDocumentoEntity> entities);

    /**
     * Convierte TipoDocumentoCreateDTO a TipoDocumentoEntity (CREAR).
     *
     * CAMPOS IGNORADOS:
     * - id: autogenerado.
     * - creadoEn: gestionado por JPA.
     * - active: el service lo establece en true al crear (US-024).
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "creadoEn",    ignore = true)
    @Mapping(target = "active",      ignore = true)
    @Mapping(target = "organizacion", source = "organizacionNit", qualifiedByName = "nitToOrganizacionEntity")
    TipoDocumentoEntity toEntity(TipoDocumentoCreateDTO createDTO);

    /**
     * Actualiza TipoDocumentoEntity existente con datos de TipoDocumentoUpdateDTO.
     *
     * CAMPOS NO ACTUALIZABLES:
     * - id, creadoEn, organizacion: inmutables.
     * - active: se gestiona exclusivamente mediante el endpoint de activar/desactivar (US-026).
     * Estrategia IGNORE para null → actualización parcial (US-025).
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "creadoEn",    ignore = true)
    @Mapping(target = "organizacion", ignore = true)
    @Mapping(target = "active",      ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(TipoDocumentoUpdateDTO updateDTO, @MappingTarget TipoDocumentoEntity entity);

    /**
     * MÉTODO AUXILIAR: Crea OrganizacionEntity con solo el NIT para establecer la FK.
     * JPA resuelve la relación correctamente sin necesidad de cargar el objeto completo.
     */
    @Named("nitToOrganizacionEntity")
    default OrganizacionEntity nitToOrganizacionEntity(Long nit) {
        if (nit == null) return null;
        OrganizacionEntity org = new OrganizacionEntity();
        org.setNit(nit);
        return org;
    }
}

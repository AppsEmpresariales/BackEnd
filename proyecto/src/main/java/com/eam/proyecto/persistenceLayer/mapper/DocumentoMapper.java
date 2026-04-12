package com.eam.proyecto.persistenceLayer.mapper;

import com.eam.proyecto.businessLayer.dto.DocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoUpdateDTO;
import com.eam.proyecto.persistenceLayer.entity.*;

import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre DocumentoEntity y DTOs usando MapStruct.
 *
 * COMPLEJIDAD ADICIONAL:
 * - DocumentoEntity tiene 4 relaciones ManyToOne:
 *     → UsuarioEntity    (creadoPor)
 *     → OrganizacionEntity (organizacion)
 *     → TipoDocumentoEntity (tipoDocumento)
 *     → EstadoDocumentoEntity (estadoDocumento) — FK real, NO enum.
 * - DocumentoDTO expone IDs y nombres denormalizados de cada relación.
 *
 * HISTORIAS CUBIERTAS:
 * - US-017: Crear documento con metadatos (RF17) → toEntity(DocumentoCreateDTO).
 * - US-018: Subir archivo adjunto (RF18) → campos archivoNombre, archivoRuta, tamanioArchivo.
 * - US-019: Editar metadatos (RF19) → updateEntityFromDTO(...).
 * - US-020: Eliminar documento (RF20) → gestionado en el service (no requiere mapper especial).
 * - US-021: Consultar documentos (RF21) → toDTOList(...).
 * - US-022: Filtrar documentos (RF22) → toDTOList sobre resultado filtrado.
 * - US-023: Descargar archivo (RF23) → campo archivoRuta del DTO.
 * - US-027: Asociar documento a tipo documental (RF27) → campo tipoDocumentoId.
 * - US-030: Cambiar estado del documento en flujo (RF30) → campo estadoDocumentoId.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface DocumentoMapper {

    /**
     * Convierte DocumentoEntity a DocumentoDTO (LECTURA).
     *
     * MAPEOS PERSONALIZADOS (denormalización):
     * - creadoPorCedula      → creadoPor.cedula
     * - creadoPorNombre      → creadoPor.nombre
     * - organizacionNit      → organizacion.nit
     * - organizacionNombre   → organizacion.nombre
     * - tipoDocumentoId      → tipoDocumento.id
     * - tipoDocumentoNombre  → tipoDocumento.nombre
     * - estadoDocumentoId    → estadoDocumento.id
     * - estadoDocumentoNombre → estadoDocumento.nombre
     */
    @Mapping(target = "creadoPorCedula",       source = "creadoPor.cedula")
    @Mapping(target = "creadoPorNombre",        source = "creadoPor.nombre")
    @Mapping(target = "organizacionNit",        source = "organizacion.nit")
    @Mapping(target = "organizacionNombre",     source = "organizacion.nombre")
    @Mapping(target = "tipoDocumentoId",        source = "tipoDocumento.id")
    @Mapping(target = "tipoDocumentoNombre",    source = "tipoDocumento.nombre")
    @Mapping(target = "estadoDocumentoId",      source = "estadoDocumento.id")
    @Mapping(target = "estadoDocumentoNombre",  source = "estadoDocumento.nombre")
    DocumentoDTO toDTO(DocumentoEntity entity);

    /**
     * Convierte lista de DocumentoEntity a lista de DocumentoDTO.
     */
    List<DocumentoDTO> toDTOList(List<DocumentoEntity> entities);

    /**
     * Convierte DocumentoCreateDTO a DocumentoEntity (CREAR).
     *
     * MAPEOS COMPLEJOS:
     * - creadoPorCedula  → UsuarioEntity (solo cedula)
     * - organizacionNit  → OrganizacionEntity (solo nit)
     * - tipoDocumentoId  → TipoDocumentoEntity (solo id)
     * - estadoDocumentoId → EstadoDocumentoEntity (solo id) — estado inicial asignado por el service.
     *
     * CAMPOS IGNORADOS:
     * - id: autogenerado.
     * - creadoEn / actualizadoEn: gestionados por JPA.
     * - version: el service lo inicializa en 1.
     */
    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "creadoEn",       ignore = true)
    @Mapping(target = "actualizadoEn",  ignore = true)
    @Mapping(target = "version",        ignore = true)
    @Mapping(target = "creadoPor",      source = "creadoPorCedula",  qualifiedByName = "cedulaToUsuarioEntity")
    @Mapping(target = "organizacion",   source = "organizacionNit",  qualifiedByName = "nitToOrganizacionEntity")
    @Mapping(target = "tipoDocumento",  source = "tipoDocumentoId",  qualifiedByName = "idToTipoDocumentoEntity")
    @Mapping(target = "estadoDocumento",source = "estadoDocumentoId",qualifiedByName = "idToEstadoDocumentoEntity")
    DocumentoEntity toEntity(DocumentoCreateDTO createDTO);

    /**
     * Actualiza DocumentoEntity existente con datos de DocumentoUpdateDTO (EDITAR METADATOS).
     *
     * CAMPOS NO ACTUALIZABLES:
     * - id, creadoEn, creadoPor, organizacion: inmutables.
     * - estadoDocumento: el estado se cambia mediante el flujo de trabajo (US-030), no aquí.
     * - Archivo (archivoNombre, archivoRuta, tamanioArchivo): gestionados por el service de storage.
     * - version: el service incrementa la versión al actualizar.
     * Estrategia IGNORE para null → actualización parcial (US-019).
     */
    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "creadoEn",        ignore = true)
    @Mapping(target = "actualizadoEn",   ignore = true)
    @Mapping(target = "creadoPor",       ignore = true)
    @Mapping(target = "organizacion",    ignore = true)
    @Mapping(target = "estadoDocumento", ignore = true)
    @Mapping(target = "archivoNombre",   ignore = true)
    @Mapping(target = "archivoRuta",     ignore = true)
    @Mapping(target = "tamanioArchivo",  ignore = true)
    @Mapping(target = "version",         ignore = true)
    @Mapping(target = "tipoDocumento",   source = "tipoDocumentoId", qualifiedByName = "idToTipoDocumentoEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(DocumentoUpdateDTO updateDTO, @MappingTarget DocumentoEntity entity);

    // ─── Métodos auxiliares ───────────────────────────────────────────────────

    /** Crea UsuarioEntity con solo la cédula para establecer la FK. */
    @Named("cedulaToUsuarioEntity")
    default UsuarioEntity cedulaToUsuarioEntity(Long cedula) {
        if (cedula == null) return null;
        UsuarioEntity u = new UsuarioEntity();
        u.setCedula(cedula);
        return u;
    }

    /** Crea OrganizacionEntity con solo el NIT para establecer la FK. */
    @Named("nitToOrganizacionEntity")
    default OrganizacionEntity nitToOrganizacionEntity(Long nit) {
        if (nit == null) return null;
        OrganizacionEntity o = new OrganizacionEntity();
        o.setNit(nit);
        return o;
    }

    /** Crea TipoDocumentoEntity con solo el ID para establecer la FK. */
    @Named("idToTipoDocumentoEntity")
    default TipoDocumentoEntity idToTipoDocumentoEntity(Long id) {
        if (id == null) return null;
        TipoDocumentoEntity t = new TipoDocumentoEntity();
        t.setId(id);
        return t;
    }

    /** Crea EstadoDocumentoEntity con solo el ID para establecer la FK. */
    @Named("idToEstadoDocumentoEntity")
    default EstadoDocumentoEntity idToEstadoDocumentoEntity(Long id) {
        if (id == null) return null;
        EstadoDocumentoEntity e = new EstadoDocumentoEntity();
        e.setId(id);
        return e;
    }
}

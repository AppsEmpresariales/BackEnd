package com.eam.proyecto.persistenceLayer.mapper;

import com.eam.proyecto.businessLayer.dto.AuditRegistroCreateDTO;
import com.eam.proyecto.businessLayer.dto.AuditRegistroDTO;
import com.eam.proyecto.persistenceLayer.entity.AuditRegistroEntity;
import com.eam.proyecto.persistenceLayer.entity.DocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre AuditRegistroEntity y DTOs usando MapStruct.
 *
 * NOTA DE DISEÑO:
 * - AuditRegistro es INMUTABLE: solo se crea, nunca se edita ni elimina.
 * - No existe updateEntityFromDTO.
 * - El service construye el registro y lo persiste directamente (append-only).
 *
 * HISTORIAS CUBIERTAS:
 * - US-033: Registrar historial de acciones por documento (RF33) → toEntity(CreateDTO).
 * - US-034: Mostrar historial de cambios de un documento (RF34) → toDTOList(...).
 * - US-035: Registrar usuario, fecha y acción en cada evento (RF35) → campos usuario + accion + creadoEn.
 * - US-036: Consultar trazabilidad completa del sistema (RF36) → toDTOList sobre resultado completo.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface AuditRegistroMapper {

    /**
     * Convierte AuditRegistroEntity a AuditRegistroDTO (LECTURA).
     *
     * MAPEOS PERSONALIZADOS:
     * - documentoId     → documento.id
     * - documentoTitulo → documento.titulo
     * - usuarioCedula   → usuario.cedula
     * - usuarioNombre   → usuario.nombre
     */
    @Mapping(target = "documentoId",     source = "documento.id")
    @Mapping(target = "documentoTitulo", source = "documento.titulo")
    @Mapping(target = "usuarioCedula",   source = "usuario.cedula")
    @Mapping(target = "usuarioNombre",   source = "usuario.nombre")
    AuditRegistroDTO toDTO(AuditRegistroEntity entity);

    /**
     * Convierte lista de AuditRegistroEntity a lista de AuditRegistroDTO.
     */
    List<AuditRegistroDTO> toDTOList(List<AuditRegistroEntity> entities);

    /**
     * Convierte AuditRegistroCreateDTO a AuditRegistroEntity (CREAR REGISTRO DE AUDITORÍA).
     *
     * CAMPOS IGNORADOS:
     * - id: autogenerado.
     * - creadoEn: el service asigna LocalDateTime.now() antes de persistir.
     */
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "creadoEn",  ignore = true)
    @Mapping(target = "documento", source = "documentoId",   qualifiedByName = "idToDocumentoEntity")
    @Mapping(target = "usuario",   source = "usuarioCedula", qualifiedByName = "cedulaToUsuarioEntity")
    AuditRegistroEntity toEntity(AuditRegistroCreateDTO createDTO);

    // ─── Métodos auxiliares ───────────────────────────────────────────────────

    @Named("idToDocumentoEntity")
    default DocumentoEntity idToDocumentoEntity(Long id) {
        if (id == null) return null;
        DocumentoEntity d = new DocumentoEntity();
        d.setId(id);
        return d;
    }

    @Named("cedulaToUsuarioEntity")
    default UsuarioEntity cedulaToUsuarioEntity(Long cedula) {
        if (cedula == null) return null;
        UsuarioEntity u = new UsuarioEntity();
        u.setCedula(cedula);
        return u;
    }
}

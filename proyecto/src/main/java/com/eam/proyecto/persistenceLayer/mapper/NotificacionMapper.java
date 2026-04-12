package com.eam.proyecto.persistenceLayer.mapper;

import com.eam.proyecto.businessLayer.dto.NotificacionCreateDTO;
import com.eam.proyecto.businessLayer.dto.NotificacionDTO;
import com.eam.proyecto.persistenceLayer.entity.DocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.NotificacionEntity;
import com.eam.proyecto.persistenceLayer.entity.PlantillaCorreoEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre NotificacionEntity y DTOs usando MapStruct.
 *
 * COMPLEJIDAD ADICIONAL:
 * - NotificacionEntity tiene 3 relaciones ManyToOne:
 *     → UsuarioEntity        (usuario destinatario)
 *     → DocumentoEntity      (documento relacionado)
 *     → PlantillaCorreoEntity (plantilla usada para generar el mensaje)
 * - Contiene el Enum CanalNotificacionEnum (EMAIL, SISTEMA).
 *   MapStruct lo mapea automáticamente por nombre.
 *
 * HISTORIAS CUBIERTAS:
 * - US-037: Enviar correo de confirmación al crear documento (RF37) → toEntity(CreateDTO).
 * - US-038: Notificar cambios de estado del documento (RF38) → toEntity(CreateDTO).
 * - US-039: Enviar alertas de tareas pendientes a usuarios asignados (RF39) → toEntity(CreateDTO).
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface NotificacionMapper {

    /**
     * Convierte NotificacionEntity a NotificacionDTO (LECTURA).
     *
     * MAPEOS PERSONALIZADOS:
     * - usuarioCedula      → usuario.cedula
     * - usuarioNombre      → usuario.nombre
     * - documentoId        → documento.id
     * - documentoTitulo    → documento.titulo
     * - plantillaId        → plantilla.id
     * - plantillaNombre    → plantilla.nombre
     */
    @Mapping(target = "usuarioCedula",   source = "usuario.cedula")
    @Mapping(target = "usuarioNombre",   source = "usuario.nombre")
    @Mapping(target = "documentoId",     source = "documento.id")
    @Mapping(target = "documentoTitulo", source = "documento.titulo")
    @Mapping(target = "plantillaId",     source = "plantilla.id")
    @Mapping(target = "plantillaNombre", source = "plantilla.nombre")
    NotificacionDTO toDTO(NotificacionEntity entity);

    /**
     * Convierte lista de NotificacionEntity a lista de NotificacionDTO.
     */
    List<NotificacionDTO> toDTOList(List<NotificacionEntity> entities);

    /**
     * Convierte NotificacionCreateDTO a NotificacionEntity (CREAR / ENVIAR NOTIFICACIÓN).
     *
     * CAMPOS IGNORADOS:
     * - id: autogenerado.
     * - enviadaA: el service asigna LocalDateTime.now() al enviar.
     * - estaLeida: se inicializa en false en el service.
     */
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "enviadaA",  ignore = true)
    @Mapping(target = "estaLeida", ignore = true)
    @Mapping(target = "usuario",   source = "usuarioCedula", qualifiedByName = "cedulaToUsuarioEntity")
    @Mapping(target = "documento", source = "documentoId",   qualifiedByName = "idToDocumentoEntity")
    @Mapping(target = "plantilla", source = "plantillaId",   qualifiedByName = "idToPlantillaEntity")
    NotificacionEntity toEntity(NotificacionCreateDTO createDTO);

    // ─── Métodos auxiliares ───────────────────────────────────────────────────

    @Named("cedulaToUsuarioEntity")
    default UsuarioEntity cedulaToUsuarioEntity(Long cedula) {
        if (cedula == null) return null;
        UsuarioEntity u = new UsuarioEntity();
        u.setCedula(cedula);
        return u;
    }

    @Named("idToDocumentoEntity")
    default DocumentoEntity idToDocumentoEntity(Long id) {
        if (id == null) return null;
        DocumentoEntity d = new DocumentoEntity();
        d.setId(id);
        return d;
    }

    @Named("idToPlantillaEntity")
    default PlantillaCorreoEntity idToPlantillaEntity(Long id) {
        if (id == null) return null;
        PlantillaCorreoEntity p = new PlantillaCorreoEntity();
        p.setId(id);
        return p;
    }
}

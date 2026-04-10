package com.eam.proyecto.persistenceLayer.mapper;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaUpdateDTO;
import com.eam.proyecto.persistenceLayer.entity.DocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoPasoEntity;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoTareaEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import com.eam.proyecto.persistenceLayer.entity.enums.EstadoTareaEnum;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre FlujoTrabajoTareaEntity y DTOs usando MapStruct.
 *
 * COMPLEJIDAD ADICIONAL:
 * - FlujoTrabajoTareaEntity tiene 3 relaciones ManyToOne:
 *     → DocumentoEntity        (documento)
 *     → FlujoTrabajoPasoEntity (paso)
 *     → UsuarioEntity          (asignadoA)
 * - Contiene un Enum propio: EstadoTareaEnum (PENDIENTE, COMPLETADO, CANCELADO).
 *   Los enums se mapean automáticamente por nombre en MapStruct.
 *
 * HISTORIAS CUBIERTAS:
 * - US-029: Asignar tareas de revisión a usuarios en el flujo (RF29) → toEntity(CreateDTO).
 * - US-030: Cambiar estado del documento durante el flujo (RF30) → updateEntityFromDTO.
 * - US-039: Enviar alertas de tareas pendientes a usuarios asignados (RF39) → campo asignadoA.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface FlujoTrabajoTareaMapper {

    /**
     * Convierte FlujoTrabajoTareaEntity a FlujoTrabajoTareaDTO (LECTURA).
     *
     * MAPEOS PERSONALIZADOS:
     * - documentoId      → documento.id
     * - documentoTitulo  → documento.titulo
     * - pasoId           → paso.id
     * - pasoNombre       → paso.nombre
     * - asignadoACedula  → asignadoA.cedula
     * - asignadoANombre  → asignadoA.nombre
     *
     * ENUM:
     * - estado: EstadoTareaEnum se mapea automáticamente a String por nombre.
     */
    @Mapping(target = "documentoId",     source = "documento.id")
    @Mapping(target = "documentoTitulo", source = "documento.titulo")
    @Mapping(target = "pasoId",          source = "paso.id")
    @Mapping(target = "pasoNombre",      source = "paso.nombre")
    @Mapping(target = "asignadoACedula", source = "asignadoA.cedula")
    @Mapping(target = "asignadoANombre", source = "asignadoA.nombre")
    FlujoTrabajoTareaDTO toDTO(FlujoTrabajoTareaEntity entity);

    /**
     * Convierte lista de FlujoTrabajoTareaEntity a lista de FlujoTrabajoTareaDTO.
     */
    List<FlujoTrabajoTareaDTO> toDTOList(List<FlujoTrabajoTareaEntity> entities);

    /**
     * Convierte FlujoTrabajoTareaCreateDTO a FlujoTrabajoTareaEntity (CREAR / ASIGNAR TAREA).
     *
     * CAMPOS IGNORADOS:
     * - id: autogenerado.
     * - creadoEn / completadoEn: gestionados por JPA / service.
     * - estado: el service lo inicializa en PENDIENTE (US-029).
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "creadoEn",    ignore = true)
    @Mapping(target = "completadoEn",ignore = true)
    @Mapping(target = "estado",      ignore = true)
    @Mapping(target = "documento",   source = "documentoId",    qualifiedByName = "idToDocumentoEntity")
    @Mapping(target = "paso",        source = "pasoId",         qualifiedByName = "idToPasoEntity")
    @Mapping(target = "asignadoA",   source = "asignadoACedula",qualifiedByName = "cedulaToUsuarioEntity")
    FlujoTrabajoTareaEntity toEntity(FlujoTrabajoTareaCreateDTO createDTO);

    /**
     * Actualiza FlujoTrabajoTareaEntity existente (CAMBIAR ESTADO / COMPLETAR TAREA).
     *
     * CAMPOS NO ACTUALIZABLES:
     * - id, documento, paso, creadoEn: inmutables.
     * - asignadoA: la tarea no se reasigna (se crearía una nueva).
     * - completadoEn: el service lo fija al momento de completar la tarea.
     * Estrategia IGNORE para null → actualización parcial (US-030).
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "documento",   ignore = true)
    @Mapping(target = "paso",        ignore = true)
    @Mapping(target = "asignadoA",   ignore = true)
    @Mapping(target = "creadoEn",    ignore = true)
    @Mapping(target = "completadoEn",ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(FlujoTrabajoTareaUpdateDTO updateDTO, @MappingTarget FlujoTrabajoTareaEntity entity);

    // ─── Métodos auxiliares ───────────────────────────────────────────────────

    @Named("idToDocumentoEntity")
    default DocumentoEntity idToDocumentoEntity(Long id) {
        if (id == null) return null;
        DocumentoEntity d = new DocumentoEntity();
        d.setId(id);
        return d;
    }

    @Named("idToPasoEntity")
    default FlujoTrabajoPasoEntity idToPasoEntity(Long id) {
        if (id == null) return null;
        FlujoTrabajoPasoEntity p = new FlujoTrabajoPasoEntity();
        p.setId(id);
        return p;
    }

    @Named("cedulaToUsuarioEntity")
    default UsuarioEntity cedulaToUsuarioEntity(Long cedula) {
        if (cedula == null) return null;
        UsuarioEntity u = new UsuarioEntity();
        u.setCedula(cedula);
        return u;
    }

    /**
     * MÉTODO AUXILIAR: Convierte String a EstadoTareaEnum.
     * Usado cuando el DTO llega con el estado como String.
     */
    @Named("stringToEstadoTareaEnum")
    default EstadoTareaEnum stringToEstadoTareaEnum(String estado) {
        return estado != null ? EstadoTareaEnum.valueOf(estado) : null;
    }
}

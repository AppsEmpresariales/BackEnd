package com.eam.proyecto.persistenceLayer.mapper;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoUpdateDTO;
import com.eam.proyecto.persistenceLayer.entity.EstadoDocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoPasoEntity;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoEntity;
import com.eam.proyecto.persistenceLayer.entity.RolEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre FlujoTrabajoPasoEntity y DTOs usando MapStruct.
 *
 * COMPLEJIDAD ADICIONAL:
 * - FlujoTrabajoPasoEntity tiene 3 relaciones:
 *     → FlujoTrabajoEntity   (flujoTrabajo)
 *     → RolEntity            (rolRequerido) — rol que debe ejecutar el paso.
 *     → EstadoDocumentoEntity (objetivoEstado) — estado al que lleva completar el paso.
 *
 * HISTORIAS CUBIERTAS:
 * - US-028: Definir flujo de aprobación — los pasos son la estructura del flujo (RF28).
 * - US-029: Asignar tareas de revisión a usuarios en el flujo (RF29) → rolRequerido.
 * - US-031: Validar secuencia del flujo documental (RF31) → campo ordenPaso.
 * - US-032: Parametrizar flujos por tipo documental (RF32) → pasos del flujo.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface FlujoTrabajoPasoMapper {

    /**
     * Convierte FlujoTrabajoPasoEntity a FlujoTrabajoPasoDTO (LECTURA).
     *
     * MAPEOS PERSONALIZADOS:
     * - flujoTrabajoId       → flujoTrabajo.id
     * - flujoTrabajoNombre   → flujoTrabajo.nombre
     * - rolRequeridoId       → rolRequerido.id
     * - rolRequeridoNombre   → rolRequerido.nombre
     * - objetivoEstadoId     → objetivoEstado.id
     * - objetivoEstadoNombre → objetivoEstado.nombre
     */
    @Mapping(target = "flujoTrabajoId",       source = "flujoTrabajo.id")
    @Mapping(target = "flujoTrabajoNombre",   source = "flujoTrabajo.nombre")
    @Mapping(target = "rolRequeridoId",       source = "rolRequerido.id")
    @Mapping(target = "rolRequeridoNombre",   source = "rolRequerido.nombre")
    @Mapping(target = "objetivoEstadoId",     source = "objetivoEstado.id")
    @Mapping(target = "objetivoEstadoNombre", source = "objetivoEstado.nombre")
    FlujoTrabajoPasoDTO toDTO(FlujoTrabajoPasoEntity entity);

    /**
     * Convierte lista de FlujoTrabajoPasoEntity a lista de FlujoTrabajoPasoDTO.
     */
    List<FlujoTrabajoPasoDTO> toDTOList(List<FlujoTrabajoPasoEntity> entities);

    /**
     * Convierte FlujoTrabajoPasoCreateDTO a FlujoTrabajoPasoEntity (CREAR).
     *
     * CAMPOS IGNORADOS:
     * - id: autogenerado.
     */
    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "flujoTrabajo",   source = "flujoTrabajoId",    qualifiedByName = "idToFlujoTrabajoEntity")
    @Mapping(target = "rolRequerido",   source = "rolRequeridoId",    qualifiedByName = "idToRolEntity")
    @Mapping(target = "objetivoEstado", source = "objetivoEstadoId",  qualifiedByName = "idToEstadoDocumentoEntity")
    FlujoTrabajoPasoEntity toEntity(FlujoTrabajoPasoCreateDTO createDTO);

    /**
     * Actualiza FlujoTrabajoPasoEntity existente con datos de FlujoTrabajoPasoUpdateDTO.
     *
     * CAMPOS NO ACTUALIZABLES:
     * - id, flujoTrabajo: inmutables.
     * Estrategia IGNORE para null → actualización parcial.
     */
    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "flujoTrabajo",   ignore = true)
    @Mapping(target = "rolRequerido",   source = "rolRequeridoId",   qualifiedByName = "idToRolEntity")
    @Mapping(target = "objetivoEstado", source = "objetivoEstadoId", qualifiedByName = "idToEstadoDocumentoEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(FlujoTrabajoPasoUpdateDTO updateDTO, @MappingTarget FlujoTrabajoPasoEntity entity);

    // ─── Métodos auxiliares ───────────────────────────────────────────────────

    @Named("idToFlujoTrabajoEntity")
    default FlujoTrabajoEntity idToFlujoTrabajoEntity(Long id) {
        if (id == null) return null;
        FlujoTrabajoEntity f = new FlujoTrabajoEntity();
        f.setId(id);
        return f;
    }

    @Named("idToRolEntity")
    default RolEntity idToRolEntity(Long id) {
        if (id == null) return null;
        RolEntity r = new RolEntity();
        r.setId(id);
        return r;
    }

    @Named("idToEstadoDocumentoEntity")
    default EstadoDocumentoEntity idToEstadoDocumentoEntity(Long id) {
        if (id == null) return null;
        EstadoDocumentoEntity e = new EstadoDocumentoEntity();
        e.setId(id);
        return e;
    }
}

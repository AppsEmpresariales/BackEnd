package com.eam.proyecto.persistenceLayer.dao;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoUpdateDTO;
import com.eam.proyecto.persistenceLayer.mapper.FlujoTrabajoPasoMapper;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoEntity;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoPasoEntity;
import com.eam.proyecto.persistenceLayer.entity.RolEntity;
import com.eam.proyecto.persistenceLayer.repository.FlujoTrabajoPasoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de pasos de flujo de trabajo.
 *
 * DESCRIPCION:
 * - FlujoTrabajoPasoEntity define cada paso del proceso de aprobación.
 * - Tiene 3 relaciones:
 *     → FlujoTrabajoEntity (flujoTrabajo) — inmutable
 *     → RolEntity (rolRequerido) — qué rol debe completar el paso
 *     → EstadoDocumentoEntity (objetivoEstado) — estado al que lleva el paso
 * - ordenPaso define la secuencia: debe ser único dentro del mismo flujo.
 *
 * HISTORIAS CUBIERTAS:
 * - US-028 (RF28): Definir flujo — los pasos son la estructura del flujo → save
 * - US-029 (RF29): Asignar tareas de revisión → rolRequerido en el paso
 * - US-031 (RF31): Validar secuencia del flujo → findPrimerPaso, findSiguientePaso
 * - US-032 (RF32): Parametrizar flujos → findByFlujoTrabajoId, update
 */
@Repository
@RequiredArgsConstructor
public class FlujoTrabajoPasoDAO {

    private final FlujoTrabajoPasoRepository flujoTrabajoPasoRepository;
    private final FlujoTrabajoPasoMapper flujoTrabajoPasoMapper;

    /**
     * Crear un nuevo paso en un flujo de trabajo.
     *
     * FLUJO:
     * 1. CreateDTO → Entity (mapper convierte flujoTrabajoId, rolRequeridoId, objetivoEstadoId)
     * 2. El service valida que el ordenPaso no esté duplicado en el flujo
     * 3. Guardar Entity → DTO
     *
     * US-028 / US-032
     */
    public FlujoTrabajoPasoDTO save(FlujoTrabajoPasoCreateDTO createDTO) {
        FlujoTrabajoPasoEntity entity = flujoTrabajoPasoMapper.toEntity(createDTO);
        return flujoTrabajoPasoMapper.toDTO(flujoTrabajoPasoRepository.save(entity));
    }

    /**
     * Buscar paso por ID.
     */
    public Optional<FlujoTrabajoPasoDTO> findById(Long id) {
        return flujoTrabajoPasoRepository.findById(id)
                .map(flujoTrabajoPasoMapper::toDTO);
    }

    /**
     * Actualizar datos de un paso existente.
     *
     * RESTRICCIONES (aplicadas por el mapper):
     * - id, flujoTrabajo son inmutables.
     *
     * US-032 / US-028
     */
    public Optional<FlujoTrabajoPasoDTO> update(Long id, FlujoTrabajoPasoUpdateDTO updateDTO) {
        return flujoTrabajoPasoRepository.findById(id)
                .map(existing -> {
                    flujoTrabajoPasoMapper.updateEntityFromDTO(updateDTO, existing);
                    return flujoTrabajoPasoMapper.toDTO(flujoTrabajoPasoRepository.save(existing));
                });
    }

    /**
     * Eliminar paso por ID.
     *
     * RESTRICCIÓN: El service debe verificar que no existan tareas activas
     * asociadas a este paso antes de eliminar.
     */
    public boolean deleteById(Long id) {
        if (flujoTrabajoPasoRepository.existsById(id)) {
            flujoTrabajoPasoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Listar todos los pasos de un flujo ordenados por orden de ejecución.
     *
     * US-028 / US-031: Obtener la secuencia completa del flujo.
     */
    public List<FlujoTrabajoPasoDTO> findByFlujoTrabajoIdOrdenados(Long flujoTrabajoId) {
        FlujoTrabajoEntity flujoRef = buildFlujoTrabajoRef(flujoTrabajoId);
        return flujoTrabajoPasoMapper.toDTOList(
                flujoTrabajoPasoRepository.findByFlujoTrabajoOrderByOrdenPasoAsc(flujoRef));
    }

    /**
     * Obtener el primer paso del flujo (menor ordenPaso).
     *
     * US-031: Al iniciar el flujo de un documento, se carga el primer paso.
     */
    public Optional<FlujoTrabajoPasoDTO> findPrimerPaso(Long flujoTrabajoId) {
        FlujoTrabajoEntity flujoRef = buildFlujoTrabajoRef(flujoTrabajoId);
        return flujoTrabajoPasoRepository.findFirstByFlujoTrabajoOrderByOrdenPasoAsc(flujoRef)
                .map(flujoTrabajoPasoMapper::toDTO);
    }

    /**
     * Obtener el siguiente paso después del orden actual.
     *
     * US-031: Al completar una tarea, avanzar al siguiente paso de la secuencia.
     * RETORNA: Optional vacío si el paso actual es el último del flujo.
     */
    public Optional<FlujoTrabajoPasoDTO> findSiguientePaso(Long flujoTrabajoId, Integer ordenActual) {
        FlujoTrabajoEntity flujoRef = buildFlujoTrabajoRef(flujoTrabajoId);
        return flujoTrabajoPasoRepository
                .findFirstByFlujoTrabajoAndOrdenPasoGreaterThanOrderByOrdenPasoAsc(flujoRef, ordenActual)
                .map(flujoTrabajoPasoMapper::toDTO);
    }

    /**
     * Buscar paso por posición exacta dentro del flujo.
     *
     * CASO DE USO: Validar que no existe ya un paso con ese número antes de crear uno nuevo.
     */
    public Optional<FlujoTrabajoPasoDTO> findByFlujoTrabajoIdAndOrdenPaso(
            Long flujoTrabajoId, Integer ordenPaso) {
        FlujoTrabajoEntity flujoRef = buildFlujoTrabajoRef(flujoTrabajoId);
        return flujoTrabajoPasoRepository.findByFlujoTrabajoAndOrdenPaso(flujoRef, ordenPaso)
                .map(flujoTrabajoPasoMapper::toDTO);
    }

    /**
     * Listar pasos que requieren un rol específico.
     *
     * US-029: Saber en qué flujos y pasos participa un rol determinado.
     */
    public List<FlujoTrabajoPasoDTO> findByRolRequeridoId(Long rolId) {
        RolEntity rolRef = new RolEntity();
        rolRef.setId(rolId);
        return flujoTrabajoPasoMapper.toDTOList(
                flujoTrabajoPasoRepository.findByRolRequerido(rolRef));
    }

    /**
     * Verificar si ya existe un paso con ese orden en el flujo.
     *
     * US-031: Garantizar unicidad del orden en el flujo antes de guardar.
     */
    public boolean existsByFlujoTrabajoIdAndOrdenPaso(Long flujoTrabajoId, Integer ordenPaso) {
        FlujoTrabajoEntity flujoRef = buildFlujoTrabajoRef(flujoTrabajoId);
        return flujoTrabajoPasoRepository.existsByFlujoTrabajoAndOrdenPaso(flujoRef, ordenPaso);
    }

    /**
     * Contar pasos de un flujo de trabajo.
     *
     * CASO DE USO: Saber cuántos pasos tiene el flujo para mostrar progreso.
     */
    public long countByFlujoTrabajoId(Long flujoTrabajoId) {
        FlujoTrabajoEntity flujoRef = buildFlujoTrabajoRef(flujoTrabajoId);
        return flujoTrabajoPasoRepository.countByFlujoTrabajo(flujoRef);
    }

    /**
     * Contar total de pasos registrados.
     */
    public long count() {
        return flujoTrabajoPasoRepository.count();
    }

    // ─── Método auxiliar privado ──────────────────────────────────────────────

    private FlujoTrabajoEntity buildFlujoTrabajoRef(Long flujoTrabajoId) {
        FlujoTrabajoEntity flujo = new FlujoTrabajoEntity();
        flujo.setId(flujoTrabajoId);
        return flujo;
    }
}

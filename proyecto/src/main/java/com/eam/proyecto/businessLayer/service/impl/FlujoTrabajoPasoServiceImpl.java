// FlujoTrabajoPasoServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoUpdateDTO;
import com.eam.proyecto.businessLayer.service.FlujoTrabajoPasoService;
import com.eam.proyecto.businessLayer.service.FlujoTrabajoService;
import com.eam.proyecto.businessLayer.service.RolService;
import com.eam.proyecto.persistenceLayer.dao.FlujoTrabajoPasoDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FlujoTrabajoPasoServiceImpl implements FlujoTrabajoPasoService {

    private final FlujoTrabajoPasoDAO flujoTrabajoPasoDAO;
    private final FlujoTrabajoService flujoTrabajoService;
    private final RolService rolService;

    /**
     * CREATE — Agregar un paso al flujo de trabajo — RF28 / RF32.
     *
     * FLUJO:
     * 1. Validar datos
     * 2. Verificar que el flujo existe
     * 3. Verificar que el rol requerido existe en el catálogo — RF29
     * 4. REGLA CRÍTICA: ordenPaso único dentro del flujo — RF31
     * 5. Persistir
     */
    @Override
    public FlujoTrabajoPasoDTO createPaso(FlujoTrabajoPasoCreateDTO createDTO) {
        log.info("Creando paso {} en flujo ID: {}", createDTO.getOrdenPaso(), createDTO.getFlujoTrabajoId());

        validatePasoData(createDTO);

        flujoTrabajoService.getFlujoTrabajoById(createDTO.getFlujoTrabajoId());
        rolService.getRolById(createDTO.getRolRequeridoId());

        // REGLA CRÍTICA: el orden debe ser único dentro del mismo flujo — RF31
        if (flujoTrabajoPasoDAO.existsByFlujoTrabajoIdAndOrdenPaso(createDTO.getFlujoTrabajoId(), createDTO.getOrdenPaso())) {
            log.warn("Orden {} duplicado en flujo ID {}", createDTO.getOrdenPaso(), createDTO.getFlujoTrabajoId());
            throw new IllegalArgumentException("Ya existe un paso con el orden " + createDTO.getOrdenPaso() + " en este flujo de trabajo");
        }

        FlujoTrabajoPasoDTO result = flujoTrabajoPasoDAO.save(createDTO);

        log.info("Paso creado exitosamente con ID: {}", result.getId());
        return result;
    }

    /**
     * READ — Buscar paso por ID.
     */
    @Override
    @Transactional(readOnly = true)
    public FlujoTrabajoPasoDTO getPasoById(Long id) {
        log.debug("Buscando paso por ID: {}", id);

        return flujoTrabajoPasoDAO.findById(id)
                .orElseThrow(() -> {
                    log.warn("Paso no encontrado con ID: {}", id);
                    return new RuntimeException("Paso del flujo no encontrado con ID: " + id);
                });
    }

    /**
     * READ PRIMER PASO — Obtener el primer paso del flujo — RF31.
     *
     * CASO DE USO: Al iniciar el flujo de aprobación de un documento.
     */
    @Override
    @Transactional(readOnly = true)
    public FlujoTrabajoPasoDTO getPrimerPaso(Long flujoTrabajoId) {
        log.debug("Buscando primer paso del flujo ID: {}", flujoTrabajoId);

        flujoTrabajoService.getFlujoTrabajoById(flujoTrabajoId);

        return flujoTrabajoPasoDAO.findPrimerPaso(flujoTrabajoId)
                .orElseThrow(() -> {
                    log.warn("El flujo ID {} no tiene pasos configurados", flujoTrabajoId);
                    return new IllegalStateException("El flujo de trabajo no tiene pasos configurados");
                });
    }

    /**
     * READ SIGUIENTE PASO — Obtener el siguiente paso después del orden actual — RF31.
     *
     * RETORNA: null si el paso actual es el último (fin del flujo).
     */
    @Override
    @Transactional(readOnly = true)
    public FlujoTrabajoPasoDTO getSiguientePaso(Long flujoTrabajoId, Integer ordenActual) {
        log.debug("Buscando siguiente paso después del orden {} en flujo ID: {}", ordenActual, flujoTrabajoId);

        if (ordenActual == null || ordenActual < 0) {
            throw new IllegalArgumentException("El orden actual del paso debe ser un número positivo");
        }

        // Optional vacío = fin del flujo (es el último paso)
        return flujoTrabajoPasoDAO.findSiguientePaso(flujoTrabajoId, ordenActual)
                .orElse(null);
    }

    /**
     * READ ALL — Listar todos los pasos del flujo ordenados — RF28 / RF31.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FlujoTrabajoPasoDTO> getPasosByFlujoTrabajo(Long flujoTrabajoId) {
        log.debug("Obteniendo pasos del flujo ID: {}", flujoTrabajoId);
        flujoTrabajoService.getFlujoTrabajoById(flujoTrabajoId);
        return flujoTrabajoPasoDAO.findByFlujoTrabajoIdOrdenados(flujoTrabajoId);
    }

    /**
     * UPDATE — Editar un paso del flujo — RF32.
     *
     * REGLA: flujoTrabajo es inmutable.
     */
    @Override
    public FlujoTrabajoPasoDTO updatePaso(Long id, FlujoTrabajoPasoUpdateDTO updateDTO) {
        log.info("Actualizando paso ID: {}", id);

        FlujoTrabajoPasoDTO existing = getPasoById(id);

        // Si se cambia el orden, verificar unicidad en el mismo flujo
        if (updateDTO.getOrdenPaso() != null && !updateDTO.getOrdenPaso().equals(existing.getOrdenPaso())) {
            if (flujoTrabajoPasoDAO.existsByFlujoTrabajoIdAndOrdenPaso(existing.getFlujoTrabajoId(), updateDTO.getOrdenPaso())) {
                throw new IllegalArgumentException("Ya existe un paso con el orden " + updateDTO.getOrdenPaso() + " en este flujo");
            }
        }

        FlujoTrabajoPasoDTO result = flujoTrabajoPasoDAO.update(id, updateDTO)
                .orElseThrow(() -> new RuntimeException("Error al actualizar paso ID: " + id));

        log.info("Paso actualizado exitosamente ID: {}", id);
        return result;
    }

    /**
     * DELETE — Eliminar paso del flujo.
     *
     * RESTRICCIÓN: Verificar que no existan tareas activas en este paso.
     */
    @Override
    public void deletePaso(Long id) {
        log.info("Eliminando paso ID: {}", id);

        getPasoById(id);

        boolean deleted = flujoTrabajoPasoDAO.deleteById(id);
        if (!deleted) {
            throw new RuntimeException("Error al eliminar paso ID: " + id);
        }

        log.info("Paso eliminado exitosamente ID: {}", id);
    }

    // ─── Validaciones privadas ────────────────────────────────────────────────

    private void validatePasoData(FlujoTrabajoPasoCreateDTO dto) {
        if (dto.getFlujoTrabajoId() == null) {
            throw new IllegalArgumentException("El flujo de trabajo es obligatorio");
        }
        if (dto.getRolRequeridoId() == null) {
            throw new IllegalArgumentException("El rol requerido para el paso es obligatorio");
        }
        if (dto.getOrdenPaso() == null || dto.getOrdenPaso() < 1) {
            throw new IllegalArgumentException("El orden del paso debe ser un número mayor a cero");
        }
        if (dto.getObjetivoEstadoId() == null) {
            throw new IllegalArgumentException("El estado objetivo del paso es obligatorio");
        }
    }
}
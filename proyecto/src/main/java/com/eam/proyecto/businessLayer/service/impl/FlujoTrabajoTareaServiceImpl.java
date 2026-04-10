// FlujoTrabajoTareaServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaUpdateDTO;
import com.eam.proyecto.businessLayer.service.*;
import com.eam.proyecto.persistenceLayer.dao.FlujoTrabajoTareaDAO;
import com.eam.proyecto.persistenceLayer.entity.enums.EstadoTareaEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FlujoTrabajoTareaServiceImpl implements FlujoTrabajoTareaService {

    private final FlujoTrabajoTareaDAO flujoTrabajoTareaDAO;
    private final DocumentoService documentoService;
    private final UsuarioService usuarioService;
    private final FlujoTrabajoPasoService flujoTrabajoPasoService;

    /**
     * ASIGNAR TAREA — Crear y asignar tarea a un usuario para ejecutar un paso — RF29.
     *
     * FLUJO:
     * 1. Validar datos obligatorios
     * 2. Verificar que el documento existe
     * 3. Verificar que el usuario asignado existe
     * 4. Verificar que el paso del flujo existe
     * 5. REGLA CRÍTICA: no asignar si el documento ya tiene una tarea PENDIENTE — RF31
     * 6. Asignar estado=PENDIENTE y creadoEn
     * 7. Persistir
     */
    @Override
    public FlujoTrabajoTareaDTO asignarTarea(FlujoTrabajoTareaCreateDTO createDTO) {
        log.info("Asignando tarea del paso ID {} sobre documento ID {} al usuario cédula {}",
                createDTO.getPasoId(), createDTO.getDocumentoId(), createDTO.getAsignadoACedula());

        validateTareaData(createDTO);

        documentoService.getDocumentoById(createDTO.getDocumentoId());
        usuarioService.getUsuarioByCedula(createDTO.getAsignadoACedula());
        flujoTrabajoPasoService.getPasoById(createDTO.getPasoId());

        // REGLA CRÍTICA: no puede haber dos tareas PENDIENTE sobre el mismo documento — RF31
        if (flujoTrabajoTareaDAO.existeTareaPendienteByDocumentoId(createDTO.getDocumentoId())) {
            log.warn("Documento ID {} ya tiene una tarea pendiente activa", createDTO.getDocumentoId());
            throw new IllegalStateException(
                    "El documento ya tiene una tarea pendiente. Complétela o cancélela antes de asignar una nueva");
        }

        // El service asigna el estado inicial y la fecha de creación
        createDTO.setEstado(EstadoTareaEnum.PENDIENTE);
        createDTO.setCreadoEn(LocalDateTime.now());

        FlujoTrabajoTareaDTO result = flujoTrabajoTareaDAO.save(createDTO);

        log.info("Tarea asignada exitosamente con ID: {}", result.getId());
        return result;
    }

    /**
     * READ — Buscar tarea por ID.
     */
    @Override
    @Transactional(readOnly = true)
    public FlujoTrabajoTareaDTO getTareaById(Long id) {
        log.debug("Buscando tarea por ID: {}", id);

        return flujoTrabajoTareaDAO.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tarea no encontrada con ID: {}", id);
                    return new RuntimeException("Tarea del flujo no encontrada con ID: " + id);
                });
    }

    /**
     * READ ACTIVA — Obtener la tarea pendiente activa de un documento — RF31.
     *
     * CASO DE USO: Antes de avanzar el flujo, saber qué tarea está actualmente en curso.
     */
    @Override
    @Transactional(readOnly = true)
    public FlujoTrabajoTareaDTO getTareaActivaByDocumento(Long documentoId) {
        log.debug("Buscando tarea activa del documento ID: {}", documentoId);

        documentoService.getDocumentoById(documentoId);

        return flujoTrabajoTareaDAO.findTareaActivaByDocumentoId(documentoId)
                .orElseThrow(() -> {
                    log.debug("Documento ID {} no tiene tarea pendiente activa", documentoId);
                    return new RuntimeException("El documento no tiene ninguna tarea pendiente activa");
                });
    }

    /**
     * READ ALL — Historial completo de tareas de un documento — RF34.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FlujoTrabajoTareaDTO> getTareasByDocumento(Long documentoId) {
        log.debug("Obteniendo tareas del documento ID: {}", documentoId);
        documentoService.getDocumentoById(documentoId);
        return flujoTrabajoTareaDAO.findByDocumentoId(documentoId);
    }

    /**
     * READ PENDIENTES — Tareas pendientes asignadas al usuario — RF39.
     *
     * CASO DE USO: Base para envío de alertas y el dashboard de tareas del usuario.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FlujoTrabajoTareaDTO> getTareasPendientesByUsuario(Long cedula) {
        log.debug("Obteniendo tareas pendientes del usuario cédula: {}", cedula);
        usuarioService.getUsuarioByCedula(cedula);
        return flujoTrabajoTareaDAO.findPendientesByUsuarioCedula(cedula);
    }

    /**
     * COMPLETAR TAREA — Marcar la tarea como COMPLETADO y avanzar el flujo — RF30.
     *
     * FLUJO:
     * 1. Verificar que la tarea existe y está PENDIENTE
     * 2. Completar mediante DAO (registra completadoEn automáticamente)
     *
     * NOTA: El cambio de estado del documento (RF30) lo orquesta
     * el controller o un service de flujo de orden superior,
     * que llama a completarTarea() y luego a documentoService.cambiarEstado().
     */
    @Override
    public FlujoTrabajoTareaDTO completarTarea(Long id, String comentario) {
        log.info("Completando tarea ID: {}", id);

        FlujoTrabajoTareaDTO tarea = getTareaById(id);

        if (tarea.getEstado() != EstadoTareaEnum.PENDIENTE) {
            log.warn("Intento de completar tarea ID {} con estado {}", id, tarea.getEstado());
            throw new IllegalStateException(
                    "Solo se pueden completar tareas en estado PENDIENTE. Estado actual: " + tarea.getEstado());
        }

        FlujoTrabajoTareaDTO result = flujoTrabajoTareaDAO.completar(id, comentario)
                .orElseThrow(() -> new RuntimeException("Error al completar tarea ID: " + id));

        log.info("Tarea completada exitosamente ID: {}", id);
        return result;
    }

    /**
     * CANCELAR TAREA — Marcar la tarea como CANCELADO.
     *
     * CASO DE USO: El administrador cancela una tarea bloqueada o incorrectamente asignada.
     */
    @Override
    public FlujoTrabajoTareaDTO cancelarTarea(Long id) {
        log.info("Cancelando tarea ID: {}", id);

        FlujoTrabajoTareaDTO tarea = getTareaById(id);

        if (tarea.getEstado() == EstadoTareaEnum.CANCELADO) {
            throw new IllegalStateException("La tarea ya se encuentra cancelada");
        }
        if (tarea.getEstado() == EstadoTareaEnum.COMPLETADO) {
            throw new IllegalStateException("No se puede cancelar una tarea que ya fue completada");
        }

        FlujoTrabajoTareaDTO result = flujoTrabajoTareaDAO.cancelar(id)
                .orElseThrow(() -> new RuntimeException("Error al cancelar tarea ID: " + id));

        log.info("Tarea cancelada exitosamente ID: {}", id);
        return result;
    }

    /**
     * UPDATE — Actualizar comentario u otros campos editables de la tarea — RF30.
     *
     * RESTRICCIONES: documento, paso, asignadoA y creadoEn son inmutables.
     */
    @Override
    public FlujoTrabajoTareaDTO updateTarea(Long id, FlujoTrabajoTareaUpdateDTO updateDTO) {
        log.info("Actualizando tarea ID: {}", id);

        getTareaById(id);

        FlujoTrabajoTareaDTO result = flujoTrabajoTareaDAO.update(id, updateDTO)
                .orElseThrow(() -> new RuntimeException("Error al actualizar tarea ID: " + id));

        log.info("Tarea actualizada exitosamente ID: {}", id);
        return result;
    }

    // ─── Validaciones privadas ────────────────────────────────────────────────

    private void validateTareaData(FlujoTrabajoTareaCreateDTO dto) {
        if (dto.getDocumentoId() == null) {
            throw new IllegalArgumentException("El ID del documento es obligatorio");
        }
        if (dto.getPasoId() == null) {
            throw new IllegalArgumentException("El paso del flujo es obligatorio");
        }
        if (dto.getAsignadoACedula() == null) {
            throw new IllegalArgumentException("La cédula del usuario asignado es obligatoria");
        }
    }
}
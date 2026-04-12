package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaUpdateDTO;
import com.eam.proyecto.businessLayer.service.FlujoTrabajoTareaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de tareas dentro del flujo de aprobación.
 *
 * ENDPOINTS:
 * - POST   /api/v1/tareas                           → Asignar tarea a usuario (RF29)
 * - GET    /api/v1/tareas/{id}                      → Obtener tarea por ID
 * - GET    /api/v1/tareas/documento/{docId}          → Listar tareas de un documento
 * - GET    /api/v1/tareas/documento/{docId}/activa   → Tarea activa del documento
 * - GET    /api/v1/tareas/usuario/{cedula}/pendientes → Tareas pendientes del usuario (RF39)
 * - PUT    /api/v1/tareas/{id}                       → Actualizar tarea
 * - PATCH  /api/v1/tareas/{id}/completar             → Completar tarea (RF31)
 * - PATCH  /api/v1/tareas/{id}/cancelar              → Cancelar tarea
 */
@RestController
@RequestMapping("/api/v1/tareas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tareas de Flujo", description = "Asignación y gestión de tareas en el flujo de aprobación — RF29, RF30, RF31, RF39")
@CrossOrigin(origins = "*")
public class FlujoTrabajoTareaController {

    private final FlujoTrabajoTareaService flujoTrabajoTareaService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * RF29 — Asignar tarea a un usuario para un paso del flujo.
     *
     * FLUJO:
     * 1. Validar que el documento existe
     * 2. Validar que el usuario pertenece al mismo tenant
     * 3. Validar que el paso pertenece al flujo del tipo documental
     * 4. Crear tarea en estado PENDIENTE
     */
    @PostMapping
    @Operation(
            summary = "Asignar tarea a usuario",
            description = "Crea una tarea de aprobación y la asigna a un usuario específico para un paso del flujo. " +
                    "La tarea se crea en estado PENDIENTE. RF29."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Tarea asignada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoTareaDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o violación del flujo"),
            @ApiResponse(responseCode = "404", description = "Documento, usuario o paso no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<FlujoTrabajoTareaDTO> asignarTarea(
            @Parameter(description = "Datos de la tarea: documento, paso y usuario asignado", required = true)
            @RequestBody FlujoTrabajoTareaCreateDTO createDTO
    ) {
        log.info("POST /api/v1/tareas - Asignando tarea para documento ID: {}", createDTO.getDocumentoId());

        try {
            FlujoTrabajoTareaDTO result = flujoTrabajoTareaService.asignarTarea(createDTO);
            log.info("Tarea asignada exitosamente con ID: {}", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al asignar tarea: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Recurso no encontrado al asignar tarea: {}", e.getMessage());
                return ResponseEntity.notFound().build();
            }
            log.error("Error al asignar tarea: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Obtener tarea por ID.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener tarea por ID",
            description = "Retorna la información completa de una tarea de aprobación."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tarea encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoTareaDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Tarea no encontrada")
    })
    public ResponseEntity<FlujoTrabajoTareaDTO> getTareaById(
            @Parameter(description = "ID de la tarea", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/tareas/{} - Buscando tarea", id);

        try {
            FlujoTrabajoTareaDTO tarea = flujoTrabajoTareaService.getTareaById(id);
            return ResponseEntity.ok(tarea);
        } catch (RuntimeException e) {
            log.warn("Tarea no encontrada con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todas las tareas de un documento (historial completo).
     */
    @GetMapping("/documento/{documentoId}")
    @Operation(
            summary = "Listar tareas de un documento",
            description = "Retorna el historial completo de tareas (todos los estados) de un documento. RF31."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de tareas del documento"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado")
    })
    public ResponseEntity<List<FlujoTrabajoTareaDTO>> getTareasByDocumento(
            @Parameter(description = "ID del documento", required = true, example = "1")
            @PathVariable Long documentoId
    ) {
        log.debug("GET /api/v1/tareas/documento/{} - Listando tareas del documento", documentoId);

        try {
            List<FlujoTrabajoTareaDTO> tareas = flujoTrabajoTareaService.getTareasByDocumento(documentoId);
            return ResponseEntity.ok(tareas);
        } catch (RuntimeException e) {
            log.warn("Documento no encontrado ID: {}", documentoId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF31 — Obtener la tarea activa (PENDIENTE) de un documento.
     * Indica en qué paso del flujo se encuentra el documento actualmente.
     */
    @GetMapping("/documento/{documentoId}/activa")
    @Operation(
            summary = "Obtener tarea activa de un documento",
            description = "Retorna la tarea actualmente en estado PENDIENTE del documento. " +
                    "Indica el paso en curso dentro del flujo de aprobación. RF31."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tarea activa encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoTareaDTO.class))
            ),
            @ApiResponse(responseCode = "404",
                    description = "El documento no tiene tareas pendientes (flujo terminado o no iniciado)")
    })
    public ResponseEntity<FlujoTrabajoTareaDTO> getTareaActivaByDocumento(
            @Parameter(description = "ID del documento", required = true, example = "1")
            @PathVariable Long documentoId
    ) {
        log.debug("GET /api/v1/tareas/documento/{}/activa - Buscando tarea activa", documentoId);

        try {
            FlujoTrabajoTareaDTO tareaActiva = flujoTrabajoTareaService.getTareaActivaByDocumento(documentoId);
            return ResponseEntity.ok(tareaActiva);
        } catch (RuntimeException e) {
            log.debug("No hay tarea activa para documento ID: {}", documentoId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF39 — Listar tareas pendientes de un usuario.
     * Usado para notificaciones y dashboard de pendientes.
     */
    @GetMapping("/usuario/{cedula}/pendientes")
    @Operation(
            summary = "Listar tareas pendientes de un usuario",
            description = "Retorna todas las tareas en estado PENDIENTE asignadas al usuario indicado. " +
                    "Usado para el dashboard de pendientes y alertas. RF39."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de tareas pendientes"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<List<FlujoTrabajoTareaDTO>> getTareasPendientesByUsuario(
            @Parameter(description = "Cédula del usuario", required = true, example = "1234567890")
            @PathVariable Long cedula
    ) {
        log.debug("GET /api/v1/tareas/usuario/{}/pendientes - Listando tareas pendientes", cedula);

        try {
            List<FlujoTrabajoTareaDTO> pendientes = flujoTrabajoTareaService.getTareasPendientesByUsuario(cedula);
            log.debug("Se encontraron {} tareas pendientes para usuario cédula: {}", pendientes.size(), cedula);
            return ResponseEntity.ok(pendientes);
        } catch (RuntimeException e) {
            log.warn("Usuario no encontrado cédula: {}", cedula);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    /**
     * Actualizar datos de una tarea (ej: fecha límite, comentario).
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar tarea",
            description = "Actualiza los datos editables de una tarea (fecha límite, comentario). " +
                    "El estado se cambia mediante los endpoints PATCH dedicados."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tarea actualizada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoTareaDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Tarea no encontrada")
    })
    public ResponseEntity<FlujoTrabajoTareaDTO> updateTarea(
            @Parameter(description = "ID de la tarea", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Datos a actualizar", required = true)
            @RequestBody FlujoTrabajoTareaUpdateDTO updateDTO
    ) {
        log.info("PUT /api/v1/tareas/{} - Actualizando tarea", id);

        try {
            FlujoTrabajoTareaDTO result = flujoTrabajoTareaService.updateTarea(id, updateDTO);
            log.info("Tarea actualizada exitosamente ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar tarea ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.warn("Tarea no encontrada para actualizar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF31 — Completar tarea y avanzar el documento al siguiente paso del flujo.
     *
     * FLUJO:
     * 1. Marcar tarea como COMPLETADA
     * 2. Registrar comentario y fecha de completado
     * 3. Cambiar el estado del documento según el objetivoEstado del paso
     * 4. Si hay siguiente paso → crear nueva tarea automáticamente
     */
    @PatchMapping("/{id}/completar")
    @Operation(
            summary = "Completar tarea de aprobación",
            description = "Marca la tarea como COMPLETADA, avanza el estado del documento y crea la " +
                    "siguiente tarea si el flujo continúa. RF31."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tarea completada y flujo avanzado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoTareaDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Tarea no encontrada"),
            @ApiResponse(responseCode = "409", description = "La tarea no está en estado PENDIENTE")
    })
    public ResponseEntity<FlujoTrabajoTareaDTO> completarTarea(
            @Parameter(description = "ID de la tarea a completar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Comentario opcional sobre la decisión")
            @RequestParam(required = false) String comentario
    ) {
        log.info("PATCH /api/v1/tareas/{}/completar - Completando tarea", id);

        try {
            FlujoTrabajoTareaDTO result = flujoTrabajoTareaService.completarTarea(id, comentario);
            log.info("Tarea completada exitosamente ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Conflicto al completar tarea ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.warn("Tarea no encontrada para completar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cancelar tarea.
     */
    @PatchMapping("/{id}/cancelar")
    @Operation(
            summary = "Cancelar tarea",
            description = "Cancela una tarea pendiente. No avanza el flujo ni cambia el estado del documento."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tarea cancelada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoTareaDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Tarea no encontrada"),
            @ApiResponse(responseCode = "409", description = "La tarea no está en estado PENDIENTE")
    })
    public ResponseEntity<FlujoTrabajoTareaDTO> cancelarTarea(
            @Parameter(description = "ID de la tarea a cancelar", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("PATCH /api/v1/tareas/{}/cancelar - Cancelando tarea", id);

        try {
            FlujoTrabajoTareaDTO result = flujoTrabajoTareaService.cancelarTarea(id);
            log.info("Tarea cancelada exitosamente ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Conflicto al cancelar tarea ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.warn("Tarea no encontrada para cancelar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
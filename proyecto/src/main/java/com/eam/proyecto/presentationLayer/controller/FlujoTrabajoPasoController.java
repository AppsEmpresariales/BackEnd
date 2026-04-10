package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoUpdateDTO;
import com.eam.proyecto.businessLayer.service.FlujoTrabajoPasoService;
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
 * Controlador REST para gestión de pasos dentro de un flujo de trabajo.
 *
 * ENDPOINTS:
 * - POST   /api/v1/flujos-trabajo-pasos                            → Crear paso (RF28)
 * - GET    /api/v1/flujos-trabajo-pasos/{id}                       → Obtener por ID
 * - GET    /api/v1/flujos-trabajo-pasos/flujo/{flujoId}            → Listar pasos del flujo
 * - GET    /api/v1/flujos-trabajo-pasos/flujo/{flujoId}/primero    → Primer paso del flujo (RF31)
 * - GET    /api/v1/flujos-trabajo-pasos/flujo/{flujoId}/siguiente/{orden} → Siguiente paso (RF31)
 * - PUT    /api/v1/flujos-trabajo-pasos/{id}                       → Actualizar paso
 * - DELETE /api/v1/flujos-trabajo-pasos/{id}                       → Eliminar paso
 */
@RestController
@RequestMapping("/api/v1/flujos-trabajo-pasos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pasos de Flujo de Trabajo", description = "Gestión de pasos del workflow de aprobación — RF28, RF31")
@CrossOrigin(origins = "*")
public class FlujoTrabajoPasoController {

    private final FlujoTrabajoPasoService flujoTrabajoPasoService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * RF28 — Agregar un paso al flujo de trabajo.
     *
     * Cada paso define: nombre, orden, rol requerido y estado objetivo.
     */
    @PostMapping
    @Operation(
            summary = "Crear paso en flujo de trabajo",
            description = "Agrega un nuevo paso a un flujo de trabajo existente. " +
                    "Cada paso tiene un orden, rol requerido y estado objetivo que alcanza el documento. RF28."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Paso creado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoPasoDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o número de orden duplicado"),
            @ApiResponse(responseCode = "404", description = "Flujo de trabajo no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<FlujoTrabajoPasoDTO> createPaso(
            @Parameter(description = "Datos del paso a crear", required = true)
            @RequestBody FlujoTrabajoPasoCreateDTO createDTO
    ) {
        log.info("POST /api/v1/flujos-trabajo-pasos - Creando paso en flujo ID: {}", createDTO.getFlujoTrabajoId());

        try {
            FlujoTrabajoPasoDTO result = flujoTrabajoPasoService.createPaso(createDTO);
            log.info("Paso creado exitosamente con ID: {}", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear paso: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Flujo de trabajo no encontrado: {}", e.getMessage());
                return ResponseEntity.notFound().build();
            }
            log.error("Error al crear paso: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Obtener paso por ID.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener paso por ID",
            description = "Retorna la información completa de un paso específico del flujo."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Paso encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoPasoDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Paso no encontrado")
    })
    public ResponseEntity<FlujoTrabajoPasoDTO> getPasoById(
            @Parameter(description = "ID del paso", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/flujos-trabajo-pasos/{} - Buscando paso", id);

        try {
            FlujoTrabajoPasoDTO paso = flujoTrabajoPasoService.getPasoById(id);
            return ResponseEntity.ok(paso);
        } catch (RuntimeException e) {
            log.warn("Paso no encontrado con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todos los pasos de un flujo de trabajo, ordenados por ordenPaso.
     */
    @GetMapping("/flujo/{flujoId}")
    @Operation(
            summary = "Listar pasos de un flujo de trabajo",
            description = "Retorna todos los pasos del flujo ordenados por número de orden. RF28."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pasos obtenida"),
            @ApiResponse(responseCode = "404", description = "Flujo no encontrado")
    })
    public ResponseEntity<List<FlujoTrabajoPasoDTO>> getPasosByFlujoTrabajo(
            @Parameter(description = "ID del flujo de trabajo", required = true, example = "1")
            @PathVariable Long flujoId
    ) {
        log.debug("GET /api/v1/flujos-trabajo-pasos/flujo/{} - Listando pasos del flujo", flujoId);

        try {
            List<FlujoTrabajoPasoDTO> pasos = flujoTrabajoPasoService.getPasosByFlujoTrabajo(flujoId);
            return ResponseEntity.ok(pasos);
        } catch (RuntimeException e) {
            log.warn("Flujo de trabajo no encontrado ID: {}", flujoId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF31 — Obtener el primer paso del flujo.
     * Usado para inicializar una tarea cuando se abre el proceso de aprobación.
     */
    @GetMapping("/flujo/{flujoId}/primero")
    @Operation(
            summary = "Obtener primer paso del flujo",
            description = "Retorna el paso con menor ordenPaso del flujo. " +
                    "Usado para iniciar el proceso de aprobación de un documento. RF31."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Primer paso encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoPasoDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "El flujo no tiene pasos configurados")
    })
    public ResponseEntity<FlujoTrabajoPasoDTO> getPrimerPaso(
            @Parameter(description = "ID del flujo de trabajo", required = true, example = "1")
            @PathVariable Long flujoId
    ) {
        log.debug("GET /api/v1/flujos-trabajo-pasos/flujo/{}/primero - Buscando primer paso", flujoId);

        try {
            FlujoTrabajoPasoDTO primerPaso = flujoTrabajoPasoService.getPrimerPaso(flujoId);
            return ResponseEntity.ok(primerPaso);
        } catch (RuntimeException e) {
            log.warn("No se encontró el primer paso para el flujo ID: {}", flujoId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF31 — Obtener el siguiente paso en la secuencia del flujo.
     * Usado para avanzar al siguiente paso tras completar una tarea.
     */
    @GetMapping("/flujo/{flujoId}/siguiente/{ordenActual}")
    @Operation(
            summary = "Obtener siguiente paso del flujo",
            description = "Retorna el paso inmediatamente posterior al orden indicado. " +
                    "Devuelve 404 si el paso actual es el último del flujo. RF31."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Siguiente paso encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoPasoDTO.class))
            ),
            @ApiResponse(responseCode = "404",
                    description = "No hay siguiente paso — el flujo ha llegado al final")
    })
    public ResponseEntity<FlujoTrabajoPasoDTO> getSiguientePaso(
            @Parameter(description = "ID del flujo de trabajo", required = true, example = "1")
            @PathVariable Long flujoId,
            @Parameter(description = "Número de orden del paso actual", required = true, example = "1")
            @PathVariable Integer ordenActual
    ) {
        log.debug("GET /api/v1/flujos-trabajo-pasos/flujo/{}/siguiente/{} - Buscando siguiente paso",
                flujoId, ordenActual);

        try {
            FlujoTrabajoPasoDTO siguiente = flujoTrabajoPasoService.getSiguientePaso(flujoId, ordenActual);
            return ResponseEntity.ok(siguiente);
        } catch (RuntimeException e) {
            log.debug("No hay siguiente paso para flujo ID {} después del orden {}", flujoId, ordenActual);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    /**
     * Actualizar paso del flujo de trabajo.
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar paso del flujo",
            description = "Actualiza los datos de un paso existente. Los campos null se ignoran."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Paso actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoPasoDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Paso no encontrado")
    })
    public ResponseEntity<FlujoTrabajoPasoDTO> updatePaso(
            @Parameter(description = "ID del paso", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Datos a actualizar", required = true)
            @RequestBody FlujoTrabajoPasoUpdateDTO updateDTO
    ) {
        log.info("PUT /api/v1/flujos-trabajo-pasos/{} - Actualizando paso", id);

        try {
            FlujoTrabajoPasoDTO result = flujoTrabajoPasoService.updatePaso(id, updateDTO);
            log.info("Paso actualizado exitosamente ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar paso ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.warn("Paso no encontrado para actualizar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    /**
     * Eliminar paso del flujo de trabajo.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar paso del flujo",
            description = "Elimina un paso del flujo de trabajo. No se puede eliminar si tiene tareas activas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Paso eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Paso no encontrado"),
            @ApiResponse(responseCode = "409", description = "El paso tiene tareas activas asociadas"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> deletePaso(
            @Parameter(description = "ID del paso a eliminar", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("DELETE /api/v1/flujos-trabajo-pasos/{} - Eliminando paso", id);

        try {
            flujoTrabajoPasoService.deletePaso(id);
            log.info("Paso eliminado exitosamente ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("No se puede eliminar paso ID {} con tareas activas", id);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Paso no encontrado para eliminar ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al eliminar paso ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoUpdateDTO;
import com.eam.proyecto.businessLayer.service.FlujoTrabajoService;
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
 * Controlador REST para gestión de flujos de trabajo (workflow).
 *
 * ENDPOINTS:
 * - POST   /api/v1/flujos-trabajo                                         → Crear flujo (RF28 / RF44)
 * - GET    /api/v1/flujos-trabajo/{id}                                    → Obtener por ID
 * - GET    /api/v1/flujos-trabajo/organizacion/{nit}                      → Listar por org
 * - GET    /api/v1/flujos-trabajo/organizacion/{nit}/activos              → Listar activos por org
 * - GET    /api/v1/flujos-trabajo/organizacion/{nit}/tipo/{tipoId}/activo → Flujo activo para tipo doc
 * - PUT    /api/v1/flujos-trabajo/{id}                                    → Actualizar flujo
 * - DELETE /api/v1/flujos-trabajo/{id}                                    → Eliminar flujo
 */
@RestController
@RequestMapping("/api/v1/flujos-trabajo")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Flujos de Trabajo", description = "Parametrización de workflows de aprobación documental — RF28, RF31, RF32, RF44")
@CrossOrigin(origins = "*")
public class FlujoTrabajoController {

    private final FlujoTrabajoService flujoTrabajoService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * RF28 / RF44 — Definir flujo de aprobación parametrizable.
     *
     * REGLA DE NEGOCIO:
     * Solo puede existir un flujo activo por tipo documental por organización (RF32).
     */
    @PostMapping
    @Operation(
            summary = "Crear flujo de trabajo",
            description = "Define un nuevo flujo de aprobación para un tipo documental en la organización. " +
                    "Solo puede existir un flujo activo por tipo documental por organización. RF28 / RF32 / RF44."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Flujo de trabajo creado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoDTO.class))
            ),
            @ApiResponse(responseCode = "400",
                    description = "Datos inválidos o ya existe un flujo activo para ese tipo documental"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<FlujoTrabajoDTO> createFlujoTrabajo(
            @Parameter(description = "Datos del flujo de trabajo a crear", required = true)
            @RequestBody FlujoTrabajoCreateDTO createDTO
    ) {
        log.info("POST /api/v1/flujos-trabajo - Creando flujo '{}' para organización NIT: {}",
                createDTO.getNombre(), createDTO.getOrganizacionNit());

        try {
            FlujoTrabajoDTO result = flujoTrabajoService.createFlujoTrabajo(createDTO);
            log.info("Flujo de trabajo creado exitosamente con ID: {}", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear flujo de trabajo: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error al crear flujo de trabajo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Obtener flujo de trabajo por ID.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener flujo de trabajo por ID",
            description = "Retorna la información completa de un flujo de trabajo específico."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Flujo encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Flujo no encontrado")
    })
    public ResponseEntity<FlujoTrabajoDTO> getFlujoTrabajoById(
            @Parameter(description = "ID del flujo de trabajo", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/flujos-trabajo/{} - Buscando flujo de trabajo", id);

        try {
            FlujoTrabajoDTO flujo = flujoTrabajoService.getFlujoTrabajoById(id);
            return ResponseEntity.ok(flujo);
        } catch (RuntimeException e) {
            log.warn("Flujo de trabajo no encontrado con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todos los flujos de trabajo de una organización.
     */
    @GetMapping("/organizacion/{nit}")
    @Operation(
            summary = "Listar flujos de trabajo por organización",
            description = "Retorna todos los flujos de trabajo (activos e inactivos) de la organización."
    )
    @ApiResponse(responseCode = "200", description = "Lista de flujos obtenida")
    public ResponseEntity<List<FlujoTrabajoDTO>> getAllFlujosByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/flujos-trabajo/organizacion/{} - Listando todos los flujos", nit);
        List<FlujoTrabajoDTO> flujos = flujoTrabajoService.getAllFlujosByOrganizacion(nit);
        return ResponseEntity.ok(flujos);
    }

    /**
     * RF44 — Listar flujos activos de una organización.
     */
    @GetMapping("/organizacion/{nit}/activos")
    @Operation(
            summary = "Listar flujos de trabajo activos",
            description = "Retorna los flujos de trabajo activos de la organización. RF44."
    )
    @ApiResponse(responseCode = "200", description = "Lista de flujos activos")
    public ResponseEntity<List<FlujoTrabajoDTO>> getFlujosActivosByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/flujos-trabajo/organizacion/{}/activos - Listando flujos activos", nit);
        List<FlujoTrabajoDTO> activos = flujoTrabajoService.getFlujosActivosByOrganizacion(nit);
        return ResponseEntity.ok(activos);
    }

    /**
     * RF32 — Obtener el flujo activo para un tipo documental específico.
     * Usado al iniciar el proceso de aprobación de un documento.
     */
    @GetMapping("/organizacion/{nit}/tipo/{tipoId}/activo")
    @Operation(
            summary = "Obtener flujo activo por tipo documental",
            description = "Retorna el único flujo activo configurado para el tipo documental indicado. " +
                    "Usado al iniciar el ciclo de aprobación de un documento. RF32."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Flujo activo encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoDTO.class))
            ),
            @ApiResponse(responseCode = "404",
                    description = "No hay flujo activo para ese tipo documental en la organización")
    })
    public ResponseEntity<FlujoTrabajoDTO> getFlujoActivoByOrganizacionAndTipoDocumento(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit,
            @Parameter(description = "ID del tipo documental", required = true, example = "1")
            @PathVariable Long tipoId
    ) {
        log.debug("GET /api/v1/flujos-trabajo/organizacion/{}/tipo/{}/activo - Buscando flujo activo", nit, tipoId);

        try {
            FlujoTrabajoDTO flujo = flujoTrabajoService.getFlujoActivoByOrganizacionAndTipoDocumento(nit, tipoId);
            return ResponseEntity.ok(flujo);
        } catch (RuntimeException e) {
            log.warn("No hay flujo activo para tipo {} en organización NIT: {}", tipoId, nit);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    /**
     * Actualizar flujo de trabajo existente.
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar flujo de trabajo",
            description = "Actualiza los datos de un flujo de trabajo existente. Los campos null se ignoran."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Flujo actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FlujoTrabajoDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Flujo no encontrado")
    })
    public ResponseEntity<FlujoTrabajoDTO> updateFlujoTrabajo(
            @Parameter(description = "ID del flujo de trabajo", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Datos a actualizar", required = true)
            @RequestBody FlujoTrabajoUpdateDTO updateDTO
    ) {
        log.info("PUT /api/v1/flujos-trabajo/{} - Actualizando flujo de trabajo", id);

        try {
            FlujoTrabajoDTO result = flujoTrabajoService.updateFlujoTrabajo(id, updateDTO);
            log.info("Flujo de trabajo actualizado exitosamente ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar flujo ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.warn("Flujo de trabajo no encontrado para actualizar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    /**
     * Eliminar flujo de trabajo.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar flujo de trabajo",
            description = "Elimina un flujo de trabajo. Solo si no tiene pasos o documentos activos asociados."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Flujo eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Flujo no encontrado"),
            @ApiResponse(responseCode = "409", description = "El flujo tiene pasos o documentos activos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> deleteFlujoTrabajo(
            @Parameter(description = "ID del flujo de trabajo", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("DELETE /api/v1/flujos-trabajo/{} - Eliminando flujo de trabajo", id);

        try {
            flujoTrabajoService.deleteFlujoTrabajo(id);
            log.info("Flujo de trabajo eliminado exitosamente ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("No se puede eliminar flujo ID {} con pasos asociados", id);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Flujo no encontrado para eliminar ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al eliminar flujo ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.PlantillaCorreoCreateDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoUpdateDTO;
import com.eam.proyecto.businessLayer.service.PlantillaCorreoService;
import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
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
 * Controlador REST para gestión de plantillas de correo electrónico.
 *
 * Cada plantilla está asociada a un evento del sistema y a una organización.
 * Eventos posibles: DOCUMENTO_CREADO, TAREA_ASIGNADA, TAREA_VENCIDA,
 *                   DOCUMENTO_APROBADO, DOCUMENTO_RECHAZADO, NOTIFICACION_GENERAL
 *
 * ENDPOINTS:
 * - POST   /api/v1/plantillas-correo                                      → Crear plantilla (RF43)
 * - GET    /api/v1/plantillas-correo/{id}                                 → Obtener por ID
 * - GET    /api/v1/plantillas-correo/organizacion/{nit}                   → Listar todas por org
 * - GET    /api/v1/plantillas-correo/organizacion/{nit}/activas           → Listar activas por org
 * - GET    /api/v1/plantillas-correo/organizacion/{nit}/evento/{evento}   → Plantilla activa por evento (RF40)
 * - PUT    /api/v1/plantillas-correo/{id}                                 → Editar plantilla (RF43)
 * - PATCH  /api/v1/plantillas-correo/{id}/activar                         → Activar plantilla
 * - PATCH  /api/v1/plantillas-correo/{id}/desactivar                      → Desactivar plantilla
 * - DELETE /api/v1/plantillas-correo/{id}                                 → Eliminar plantilla
 */
@RestController
@RequestMapping("/api/v1/plantillas-correo")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Plantillas de Correo", description = "Gestión de plantillas de email configurables por evento — RF40, RF43")
@CrossOrigin(origins = "*")
public class PlantillaCorreoController {

    private final PlantillaCorreoService plantillaCorreoService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * RF43 — Crear plantilla de correo para un evento del sistema.
     *
     * REGLAS DE NEGOCIO:
     * - Solo puede haber una plantilla activa por evento por organización.
     */
    @PostMapping
    @Operation(
            summary = "Crear plantilla de correo",
            description = "Crea una nueva plantilla de email para un evento específico de la organización. " +
                    "Solo puede haber una plantilla activa por tipo de evento por organización. RF43."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Plantilla creada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PlantillaCorreoDTO.class))
            ),
            @ApiResponse(responseCode = "400",
                    description = "Datos inválidos o ya existe plantilla activa para ese evento"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<PlantillaCorreoDTO> createPlantillaCorreo(
            @Parameter(description = "Datos de la plantilla a crear", required = true)
            @RequestBody PlantillaCorreoCreateDTO createDTO
    ) {
        log.info("POST /api/v1/plantillas-correo - Creando plantilla para organización NIT: {}",
                createDTO.getOrganizacionNit());

        try {
            PlantillaCorreoDTO result = plantillaCorreoService.createPlantillaCorreo(createDTO);
            log.info("Plantilla de correo creada exitosamente con ID: {}", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear plantilla de correo: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error al crear plantilla de correo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Obtener plantilla de correo por ID.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener plantilla de correo por ID",
            description = "Retorna la información completa de una plantilla de correo específica."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Plantilla encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PlantillaCorreoDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada")
    })
    public ResponseEntity<PlantillaCorreoDTO> getPlantillaCorreoById(
            @Parameter(description = "ID de la plantilla", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/plantillas-correo/{} - Buscando plantilla", id);

        try {
            PlantillaCorreoDTO plantilla = plantillaCorreoService.getPlantillaCorreoById(id);
            return ResponseEntity.ok(plantilla);
        } catch (RuntimeException e) {
            log.warn("Plantilla de correo no encontrada con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todas las plantillas de correo de una organización.
     */
    @GetMapping("/organizacion/{nit}")
    @Operation(
            summary = "Listar plantillas de correo por organización",
            description = "Retorna todas las plantillas de correo (activas e inactivas) de la organización."
    )
    @ApiResponse(responseCode = "200", description = "Lista de plantillas obtenida")
    public ResponseEntity<List<PlantillaCorreoDTO>> getAllPlantillasByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/plantillas-correo/organizacion/{} - Listando todas las plantillas", nit);
        List<PlantillaCorreoDTO> plantillas = plantillaCorreoService.getAllPlantillasByOrganizacion(nit);
        return ResponseEntity.ok(plantillas);
    }

    /**
     * Listar solo plantillas activas de una organización.
     */
    @GetMapping("/organizacion/{nit}/activas")
    @Operation(
            summary = "Listar plantillas activas de correo",
            description = "Retorna únicamente las plantillas activas de la organización."
    )
    @ApiResponse(responseCode = "200", description = "Lista de plantillas activas")
    public ResponseEntity<List<PlantillaCorreoDTO>> getPlantillasActivasByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/plantillas-correo/organizacion/{}/activas - Listando plantillas activas", nit);
        List<PlantillaCorreoDTO> activas = plantillaCorreoService.getPlantillasActivasByOrganizacion(nit);
        return ResponseEntity.ok(activas);
    }

    /**
     * RF40 — Obtener la plantilla activa para un evento específico.
     * Usado por el servicio de notificaciones para enviar correos con la plantilla correcta.
     *
     * Eventos: DOCUMENTO_CREADO, TAREA_ASIGNADA, TAREA_VENCIDA,
     *          DOCUMENTO_APROBADO, DOCUMENTO_RECHAZADO, NOTIFICACION_GENERAL
     */
    @GetMapping("/organizacion/{nit}/evento/{tipoEvento}")
    @Operation(
            summary = "Obtener plantilla activa por evento",
            description = "Retorna la plantilla activa configurada para el tipo de evento en la organización. " +
                    "Eventos válidos: DOCUMENTO_CREADO, TAREA_ASIGNADA, TAREA_VENCIDA, " +
                    "DOCUMENTO_APROBADO, DOCUMENTO_RECHAZADO, NOTIFICACION_GENERAL. RF40."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Plantilla activa encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PlantillaCorreoDTO.class))
            ),
            @ApiResponse(responseCode = "404",
                    description = "No hay plantilla activa para ese evento en la organización"),
            @ApiResponse(responseCode = "400", description = "Tipo de evento inválido")
    })
    public ResponseEntity<PlantillaCorreoDTO> getPlantillaActivaByOrganizacionAndEvento(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit,
            @Parameter(
                    description = "Tipo de evento: DOCUMENTO_CREADO, TAREA_ASIGNADA, TAREA_VENCIDA, " +
                            "DOCUMENTO_APROBADO, DOCUMENTO_RECHAZADO, NOTIFICACION_GENERAL",
                    required = true,
                    example = "DOCUMENTO_CREADO"
            )
            @PathVariable TipoEventoEnum tipoEvento
    ) {
        log.debug("GET /api/v1/plantillas-correo/organizacion/{}/evento/{} - Buscando plantilla activa",
                nit, tipoEvento);

        try {
            PlantillaCorreoDTO plantilla = plantillaCorreoService
                    .getPlantillaActivaByOrganizacionAndEvento(nit, tipoEvento);
            return ResponseEntity.ok(plantilla);
        } catch (RuntimeException e) {
            log.warn("No hay plantilla activa para evento {} en organización NIT: {}", tipoEvento, nit);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    /**
     * RF43 — Editar plantilla de correo.
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Editar plantilla de correo",
            description = "Actualiza el contenido y configuración de una plantilla de correo. " +
                    "Los campos null se ignoran. RF43."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Plantilla actualizada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PlantillaCorreoDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada")
    })
    public ResponseEntity<PlantillaCorreoDTO> updatePlantillaCorreo(
            @Parameter(description = "ID de la plantilla", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Datos a actualizar", required = true)
            @RequestBody PlantillaCorreoUpdateDTO updateDTO
    ) {
        log.info("PUT /api/v1/plantillas-correo/{} - Actualizando plantilla", id);

        try {
            PlantillaCorreoDTO result = plantillaCorreoService.updatePlantillaCorreo(id, updateDTO);
            log.info("Plantilla de correo actualizada exitosamente ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar plantilla ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.warn("Plantilla no encontrada para actualizar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Activar plantilla de correo.
     */
    @PatchMapping("/{id}/activar")
    @Operation(
            summary = "Activar plantilla de correo",
            description = "Activa una plantilla de correo para que sea usada en el envío de notificaciones. " +
                    "Solo puede haber una activa por evento por organización."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plantilla activada"),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada"),
            @ApiResponse(responseCode = "409", description = "Ya está activa o hay conflicto con otra plantilla activa")
    })
    public ResponseEntity<PlantillaCorreoDTO> activarPlantilla(
            @Parameter(description = "ID de la plantilla", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("PATCH /api/v1/plantillas-correo/{}/activar - Activando plantilla", id);

        try {
            PlantillaCorreoDTO result = plantillaCorreoService.activarPlantilla(id);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Conflicto al activar plantilla ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.warn("Plantilla no encontrada para activar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Desactivar plantilla de correo.
     */
    @PatchMapping("/{id}/desactivar")
    @Operation(
            summary = "Desactivar plantilla de correo",
            description = "Desactiva una plantilla de correo. Los correos futuros del evento no se enviarán " +
                    "hasta configurar otra plantilla activa."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plantilla desactivada"),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada"),
            @ApiResponse(responseCode = "409", description = "Ya está inactiva")
    })
    public ResponseEntity<PlantillaCorreoDTO> desactivarPlantilla(
            @Parameter(description = "ID de la plantilla", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("PATCH /api/v1/plantillas-correo/{}/desactivar - Desactivando plantilla", id);

        try {
            PlantillaCorreoDTO result = plantillaCorreoService.desactivarPlantilla(id);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Conflicto al desactivar plantilla ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.warn("Plantilla no encontrada para desactivar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    /**
     * Eliminar plantilla de correo.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar plantilla de correo",
            description = "Elimina físicamente una plantilla de correo del sistema."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Plantilla eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Plantilla no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> deletePlantillaCorreo(
            @Parameter(description = "ID de la plantilla", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("DELETE /api/v1/plantillas-correo/{} - Eliminando plantilla", id);

        try {
            plantillaCorreoService.deletePlantillaCorreo(id);
            log.info("Plantilla de correo eliminada exitosamente ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrada")) {
                log.warn("Plantilla no encontrada para eliminar ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al eliminar plantilla ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
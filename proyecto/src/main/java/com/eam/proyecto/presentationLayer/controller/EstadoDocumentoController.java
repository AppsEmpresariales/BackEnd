package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.EstadoDocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.EstadoDocumentoDTO;
import com.eam.proyecto.businessLayer.dto.EstadoDocumentoUpdateDTO;
import com.eam.proyecto.businessLayer.service.EstadoDocumentoService;
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
 * Controlador REST para parametrización de estados de documentos.
 *
 * ENDPOINTS:
 * - POST   /api/v1/estados-documento                             → Crear estado (RF41)
 * - GET    /api/v1/estados-documento/{id}                        → Obtener por ID
 * - GET    /api/v1/estados-documento/organizacion/{nit}          → Listar por organización
 * - GET    /api/v1/estados-documento/organizacion/{nit}/iniciales → Estado inicial del tenant
 * - GET    /api/v1/estados-documento/organizacion/{nit}/finales  → Estados finales del tenant
 * - PUT    /api/v1/estados-documento/{id}                        → Editar estado
 * - DELETE /api/v1/estados-documento/{id}                        → Eliminar estado
 */
@RestController
@RequestMapping("/api/v1/estados-documento")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Estados de Documento", description = "Parametrización de estados del ciclo de vida documental — RF30, RF41")
@CrossOrigin(origins = "*")
public class EstadoDocumentoController {

    private final EstadoDocumentoService estadoDocumentoService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * RF41 — Crear nuevo estado de documento para una organización.
     *
     * REGLAS DE NEGOCIO:
     * - Solo puede haber un estado con esInicial=true por organización.
     * - El nombre debe ser único dentro del mismo tenant.
     */
    @PostMapping
    @Operation(
            summary = "Crear estado de documento",
            description = "Crea un nuevo estado parametrizable para el ciclo documental de la organización. " +
                    "Solo puede existir un estado inicial por organización. RF41."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Estado creado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EstadoDocumentoDTO.class))
            ),
            @ApiResponse(responseCode = "400",
                    description = "Datos inválidos o ya existe un estado inicial en esta organización"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<EstadoDocumentoDTO> createEstadoDocumento(
            @Parameter(description = "Datos del estado a crear", required = true)
            @RequestBody EstadoDocumentoCreateDTO createDTO
    ) {
        log.info("POST /api/v1/estados-documento - Creando estado '{}' para organización NIT: {}",
                createDTO.getNombre(), createDTO.getOrganizacionNit());

        try {
            EstadoDocumentoDTO result = estadoDocumentoService.createEstadoDocumento(createDTO);
            log.info("Estado de documento creado exitosamente con ID: {}", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear estado: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error al crear estado de documento: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Obtener estado de documento por ID.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener estado de documento por ID",
            description = "Retorna la información completa de un estado de documento específico."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EstadoDocumentoDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    public ResponseEntity<EstadoDocumentoDTO> getEstadoDocumentoById(
            @Parameter(description = "ID del estado", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/estados-documento/{} - Buscando estado", id);

        try {
            EstadoDocumentoDTO estado = estadoDocumentoService.getEstadoDocumentoById(id);
            return ResponseEntity.ok(estado);
        } catch (RuntimeException e) {
            log.warn("Estado de documento no encontrado con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todos los estados de documentos de una organización.
     */
    @GetMapping("/organizacion/{nit}")
    @Operation(
            summary = "Listar estados de documento por organización",
            description = "Retorna todos los estados configurados para una organización."
    )
    @ApiResponse(responseCode = "200", description = "Lista de estados obtenida")
    public ResponseEntity<List<EstadoDocumentoDTO>> getEstadosByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/estados-documento/organizacion/{} - Listando estados", nit);
        List<EstadoDocumentoDTO> estados = estadoDocumentoService.getEstadosByOrganizacion(nit);
        return ResponseEntity.ok(estados);
    }

    /**
     * RF31 — Obtener el estado inicial de una organización.
     * Usado al crear documentos para asignar el estado por defecto.
     */
    @GetMapping("/organizacion/{nit}/inicial")
    @Operation(
            summary = "Obtener estado inicial de la organización",
            description = "Retorna el estado marcado como esInicial=true para la organización. " +
                    "Usado en la creación de documentos. RF31."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado inicial encontrado"),
            @ApiResponse(responseCode = "404", description = "No hay estado inicial configurado para esta organización")
    })
    public ResponseEntity<EstadoDocumentoDTO> getEstadoInicialByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/estados-documento/organizacion/{}/inicial - Buscando estado inicial", nit);

        try {
            EstadoDocumentoDTO estadoInicial = estadoDocumentoService.getEstadoInicialByOrganizacion(nit);
            return ResponseEntity.ok(estadoInicial);
        } catch (RuntimeException e) {
            log.warn("No hay estado inicial configurado para organización NIT: {}", nit);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar estados finales de una organización.
     * Un estado final es aquel donde el flujo termina (APROBADO, RECHAZADO).
     */
    @GetMapping("/organizacion/{nit}/finales")
    @Operation(
            summary = "Listar estados finales de la organización",
            description = "Retorna los estados marcados como esFinal=true para la organización. " +
                    "Representan los puntos de fin del flujo documental."
    )
    @ApiResponse(responseCode = "200", description = "Lista de estados finales")
    public ResponseEntity<List<EstadoDocumentoDTO>> getEstadosFinalesByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/estados-documento/organizacion/{}/finales - Listando estados finales", nit);
        List<EstadoDocumentoDTO> finales = estadoDocumentoService.getEstadosFinalesByOrganizacion(nit);
        return ResponseEntity.ok(finales);
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    /**
     * Editar estado de documento.
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Editar estado de documento",
            description = "Actualiza los datos de un estado de documento existente. Los campos null se ignoran."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EstadoDocumentoDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Estado no encontrado")
    })
    public ResponseEntity<EstadoDocumentoDTO> updateEstadoDocumento(
            @Parameter(description = "ID del estado", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Datos a actualizar", required = true)
            @RequestBody EstadoDocumentoUpdateDTO updateDTO
    ) {
        log.info("PUT /api/v1/estados-documento/{} - Actualizando estado", id);

        try {
            EstadoDocumentoDTO result = estadoDocumentoService.updateEstadoDocumento(id, updateDTO);
            log.info("Estado actualizado exitosamente ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar estado ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.warn("Estado no encontrado para actualizar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    /**
     * Eliminar estado de documento.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar estado de documento",
            description = "Elimina un estado del catálogo de la organización. " +
                    "No se puede eliminar si hay documentos en ese estado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Estado eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Estado no encontrado"),
            @ApiResponse(responseCode = "409", description = "Hay documentos en este estado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> deleteEstadoDocumento(
            @Parameter(description = "ID del estado a eliminar", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("DELETE /api/v1/estados-documento/{} - Eliminando estado", id);

        try {
            estadoDocumentoService.deleteEstadoDocumento(id);
            log.info("Estado de documento eliminado exitosamente ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("No se puede eliminar estado ID {} con documentos asociados", id);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Estado no encontrado para eliminar ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al eliminar estado ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
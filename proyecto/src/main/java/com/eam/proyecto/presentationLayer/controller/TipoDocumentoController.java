package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.TipoDocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.TipoDocumentoDTO;
import com.eam.proyecto.businessLayer.dto.TipoDocumentoUpdateDTO;
import com.eam.proyecto.businessLayer.service.TipoDocumentoService;
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
 * Controlador REST para gestión de tipos documentales por organización.
 *
 * ENDPOINTS:
 * - POST   /api/v1/tipos-documentales                              → Crear tipo (RF24 / RF42)
 * - GET    /api/v1/tipos-documentales/{id}                         → Obtener por ID
 * - GET    /api/v1/tipos-documentales/organizacion/{nit}           → Listar todos por org
 * - GET    /api/v1/tipos-documentales/organizacion/{nit}/activos   → Listar activos por org
 * - GET    /api/v1/tipos-documentales/{id}/organizacion/{nit}      → Obtener con validación tenant
 * - PUT    /api/v1/tipos-documentales/{id}                         → Editar tipo (RF25)
 * - PATCH  /api/v1/tipos-documentales/{id}/activar                 → Activar tipo
 * - PATCH  /api/v1/tipos-documentales/{id}/desactivar              → Desactivar tipo (RF26)
 * - DELETE /api/v1/tipos-documentales/{id}                         → Eliminar tipo (RF26)
 */
@RestController
@RequestMapping("/api/v1/tipos-documentales")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tipos Documentales", description = "Gestión de tipos de documentos por organización — RF24, RF25, RF26, RF27, RF42")
@CrossOrigin(origins = "*")
public class TipoDocumentoController {

    private final TipoDocumentoService tipoDocumentoService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * RF24 / RF42 — Crear tipo documental dentro de una organización.
     * Ejemplos: Factura, Contrato, Acta, etc.
     */
    @PostMapping
    @Operation(
            summary = "Crear tipo documental",
            description = "Crea un nuevo tipo documental en la organización. " +
                    "El nombre debe ser único dentro del mismo tenant. RF24 / RF42."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Tipo documental creado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TipoDocumentoDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o nombre duplicado en el tenant"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<TipoDocumentoDTO> createTipoDocumento(
            @Parameter(description = "Datos del tipo documental a crear", required = true)
            @RequestBody TipoDocumentoCreateDTO createDTO
    ) {
        log.info("POST /api/v1/tipos-documentales - Creando tipo '{}' para organización NIT: {}",
                createDTO.getNombre(), createDTO.getOrganizacionNit());

        try {
            TipoDocumentoDTO result = tipoDocumentoService.createTipoDocumento(createDTO);
            log.info("Tipo documental creado exitosamente con ID: {}", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear tipo documental: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error al crear tipo documental: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Obtener tipo documental por ID.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener tipo documental por ID",
            description = "Retorna la información de un tipo documental específico."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tipo documental encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TipoDocumentoDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Tipo documental no encontrado")
    })
    public ResponseEntity<TipoDocumentoDTO> getTipoDocumentoById(
            @Parameter(description = "ID del tipo documental", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/tipos-documentales/{} - Buscando tipo documental", id);

        try {
            TipoDocumentoDTO tipo = tipoDocumentoService.getTipoDocumentoById(id);
            return ResponseEntity.ok(tipo);
        } catch (RuntimeException e) {
            log.warn("Tipo documental no encontrado con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF27 — Obtener tipo documental por ID con validación de tenant.
     */
    @GetMapping("/{id}/organizacion/{nit}")
    @Operation(
            summary = "Obtener tipo documental con validación de tenant",
            description = "Retorna el tipo documental solo si pertenece a la organización indicada. RF27 / RF10."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo documental encontrado en la organización"),
            @ApiResponse(responseCode = "404", description = "No encontrado en esta organización")
    })
    public ResponseEntity<TipoDocumentoDTO> getTipoDocumentoByIdAndOrganizacion(
            @Parameter(description = "ID del tipo documental", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/tipos-documentales/{}/organizacion/{} - Validando tenant", id, nit);

        try {
            TipoDocumentoDTO tipo = tipoDocumentoService.getTipoDocumentoByIdAndOrganizacion(id, nit);
            return ResponseEntity.ok(tipo);
        } catch (RuntimeException e) {
            log.warn("Tipo documental ID {} no encontrado en organización NIT: {}", id, nit);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todos los tipos documentales de una organización.
     */
    @GetMapping("/organizacion/{nit}")
    @Operation(
            summary = "Listar tipos documentales por organización",
            description = "Retorna todos los tipos documentales (activos e inactivos) de la organización."
    )
    @ApiResponse(responseCode = "200", description = "Lista de tipos documentales")
    public ResponseEntity<List<TipoDocumentoDTO>> getAllTiposByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/tipos-documentales/organizacion/{} - Listando todos los tipos", nit);
        List<TipoDocumentoDTO> tipos = tipoDocumentoService.getAllTiposByOrganizacion(nit);
        return ResponseEntity.ok(tipos);
    }

    /**
     * RF42 — Listar solo tipos documentales activos de una organización.
     */
    @GetMapping("/organizacion/{nit}/activos")
    @Operation(
            summary = "Listar tipos documentales activos",
            description = "Retorna únicamente los tipos documentales activos de la organización. RF42."
    )
    @ApiResponse(responseCode = "200", description = "Lista de tipos documentales activos")
    public ResponseEntity<List<TipoDocumentoDTO>> getTiposActivosByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/tipos-documentales/organizacion/{}/activos - Listando tipos activos", nit);
        List<TipoDocumentoDTO> activos = tipoDocumentoService.getTiposActivosByOrganizacion(nit);
        return ResponseEntity.ok(activos);
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    /**
     * RF25 — Editar tipo documental.
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Editar tipo documental",
            description = "Actualiza los datos de un tipo documental existente. Los campos null se ignoran. RF25."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tipo documental actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TipoDocumentoDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Tipo documental no encontrado")
    })
    public ResponseEntity<TipoDocumentoDTO> updateTipoDocumento(
            @Parameter(description = "ID del tipo documental", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Datos a actualizar", required = true)
            @RequestBody TipoDocumentoUpdateDTO updateDTO
    ) {
        log.info("PUT /api/v1/tipos-documentales/{} - Actualizando tipo documental", id);

        try {
            TipoDocumentoDTO result = tipoDocumentoService.updateTipoDocumento(id, updateDTO);
            log.info("Tipo documental actualizado exitosamente ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar tipo documental ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.warn("Tipo documental no encontrado para actualizar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Activar tipo documental previamente desactivado.
     */
    @PatchMapping("/{id}/activar")
    @Operation(
            summary = "Activar tipo documental",
            description = "Reactiva un tipo documental desactivado para que pueda usarse en nuevos documentos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo documental activado"),
            @ApiResponse(responseCode = "404", description = "No encontrado"),
            @ApiResponse(responseCode = "409", description = "Ya estaba activo")
    })
    public ResponseEntity<TipoDocumentoDTO> activarTipoDocumento(
            @Parameter(description = "ID del tipo documental", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("PATCH /api/v1/tipos-documentales/{}/activar - Activando tipo documental", id);

        try {
            TipoDocumentoDTO result = tipoDocumentoService.activarTipoDocumento(id);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Conflicto al activar tipo documental ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.warn("Tipo documental no encontrado para activar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF26 — Desactivar tipo documental (eliminación lógica).
     */
    @PatchMapping("/{id}/desactivar")
    @Operation(
            summary = "Desactivar tipo documental",
            description = "Desactiva un tipo documental sin eliminarlo físicamente. " +
                    "Los documentos existentes conservan su tipo. RF26."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo documental desactivado"),
            @ApiResponse(responseCode = "404", description = "No encontrado"),
            @ApiResponse(responseCode = "409", description = "Ya estaba inactivo")
    })
    public ResponseEntity<TipoDocumentoDTO> desactivarTipoDocumento(
            @Parameter(description = "ID del tipo documental", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("PATCH /api/v1/tipos-documentales/{}/desactivar - Desactivando tipo documental", id);

        try {
            TipoDocumentoDTO result = tipoDocumentoService.desactivarTipoDocumento(id);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Conflicto al desactivar tipo documental ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.warn("Tipo documental no encontrado para desactivar ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    /**
     * RF26 — Eliminar tipo documental físicamente.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar tipo documental",
            description = "Elimina físicamente un tipo documental. " +
                    "Solo si no tiene documentos asociados. RF26."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tipo documental eliminado"),
            @ApiResponse(responseCode = "404", description = "No encontrado"),
            @ApiResponse(responseCode = "409", description = "Tiene documentos asociados, usar desactivar"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> deleteTipoDocumento(
            @Parameter(description = "ID del tipo documental", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("DELETE /api/v1/tipos-documentales/{} - Eliminando tipo documental", id);

        try {
            tipoDocumentoService.deleteTipoDocumento(id);
            log.info("Tipo documental eliminado exitosamente ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("No se puede eliminar tipo documental ID {} con documentos asociados", id);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Tipo documental no encontrado para eliminar ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al eliminar tipo documental ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
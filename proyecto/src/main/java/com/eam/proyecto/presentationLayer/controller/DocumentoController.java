package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.DocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoUpdateDTO;
import com.eam.proyecto.businessLayer.service.DocumentoService;
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
 * Controlador REST para gestión del ciclo de vida de documentos.
 *
 * ENDPOINTS:
 * - POST   /api/v1/documentos                                    → Crear documento (RF17)
 * - GET    /api/v1/documentos/{id}                               → Obtener documento por ID (RF21)
 * - GET    /api/v1/documentos/organizacion/{nit}                 → Listar por organización (RF21)
 * - GET    /api/v1/documentos/{id}/organizacion/{nit}            → Obtener doc restringido al tenant (RF10)
 * - PUT    /api/v1/documentos/{id}                               → Editar metadatos (RF19)
 * - PATCH  /api/v1/documentos/{id}/estado/{estadoId}             → Cambiar estado (RF30)
 * - DELETE /api/v1/documentos/{id}                               → Eliminar documento (RF20)
 */
@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Documentos", description = "Ciclo de vida de documentos digitales — RF17, RF18, RF19, RF20, RF21, RF22, RF23, RF30")
@CrossOrigin(origins = "*")
public class DocumentoController {

    private final DocumentoService documentoService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * RF17 — Crear documento con metadatos.
     *
     * FLUJO:
     * 1. Validar datos obligatorios
     * 2. Verificar organización activa (RF10)
     * 3. Verificar que el creador pertenece al tenant
     * 4. Verificar tipo documental en el tenant (RF27)
     * 5. Asignar estado inicial automáticamente (RF31)
     * 6. Inicializar version=1
     */
    @PostMapping
    @Operation(
            summary = "Crear nuevo documento",
            description = "Crea un documento con sus metadatos. El estado inicial se asigna automáticamente " +
                    "desde la configuración de la organización. RF17 / RF27 / RF31."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Documento creado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DocumentoDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Organización, usuario o tipo documental no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<DocumentoDTO> createDocumento(
            @Parameter(description = "Datos del documento a crear", required = true)
            @RequestBody DocumentoCreateDTO createDTO
    ) {
        log.info("POST /api/v1/documentos - Creando documento '{}' en organización NIT: {}",
                createDTO.getTitulo(), createDTO.getOrganizacionNit());

        try {
            DocumentoDTO result = documentoService.createDocumento(createDTO);
            log.info("Documento creado exitosamente con ID: {}", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear documento: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrad")) {
                log.warn("Recurso no encontrado al crear documento: {}", e.getMessage());
                return ResponseEntity.notFound().build();
            }
            log.error("Error al crear documento: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * RF21 — Obtener documento por ID (sin restricción de tenant).
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener documento por ID",
            description = "Retorna la información completa de un documento. RF21."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Documento encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DocumentoDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado")
    })
    public ResponseEntity<DocumentoDTO> getDocumentoById(
            @Parameter(description = "ID del documento", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/documentos/{} - Buscando documento", id);

        try {
            DocumentoDTO documento = documentoService.getDocumentoById(id);
            return ResponseEntity.ok(documento);
        } catch (RuntimeException e) {
            log.warn("Documento no encontrado con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF10 / RF21 — Obtener documento por ID restringido al tenant.
     * Garantiza aislamiento multi-tenant: un tenant no puede ver documentos de otro.
     */
    @GetMapping("/{id}/organizacion/{nit}")
    @Operation(
            summary = "Obtener documento por ID dentro de una organización",
            description = "Retorna el documento solo si pertenece a la organización indicada. " +
                    "Garantiza el aislamiento multi-tenant. RF10 / RF21."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documento encontrado en la organización"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado en esta organización")
    })
    public ResponseEntity<DocumentoDTO> getDocumentoByIdAndOrganizacion(
            @Parameter(description = "ID del documento", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/documentos/{}/organizacion/{} - Buscando documento con aislamiento tenant", id, nit);

        try {
            DocumentoDTO documento = documentoService.getDocumentoByIdAndOrganizacion(id, nit);
            return ResponseEntity.ok(documento);
        } catch (RuntimeException e) {
            log.warn("Documento ID {} no encontrado en organización NIT: {}", id, nit);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF21 / RF22 — Listar documentos de una organización.
     */
    @GetMapping("/organizacion/{nit}")
    @Operation(
            summary = "Listar documentos por organización",
            description = "Retorna todos los documentos de la organización. RF21 / RF22."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de documentos obtenida"),
            @ApiResponse(responseCode = "404", description = "Organización no encontrada")
    })
    public ResponseEntity<List<DocumentoDTO>> getDocumentosByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/documentos/organizacion/{} - Listando documentos", nit);

        try {
            List<DocumentoDTO> documentos = documentoService.getDocumentosByOrganizacion(nit);
            log.debug("Se encontraron {} documentos para la organización NIT: {}", documentos.size(), nit);
            return ResponseEntity.ok(documentos);
        } catch (RuntimeException e) {
            log.warn("Organización no encontrada NIT: {}", nit);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    /**
     * RF19 — Editar metadatos del documento.
     *
     * RESTRICCIONES:
     * - id, creadoEn, creadoPor, organización son inmutables
     * - El estado se cambia SOLO mediante PATCH /{id}/estado/{estadoId}
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Editar metadatos del documento",
            description = "Actualiza los metadatos de un documento (título, descripción, etc.). " +
                    "El estado, el creador y la organización no se pueden modificar aquí. RF19."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Documento actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DocumentoDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado")
    })
    public ResponseEntity<DocumentoDTO> updateDocumento(
            @Parameter(description = "ID del documento a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Metadatos a actualizar (campos null se ignoran)", required = true)
            @RequestBody DocumentoUpdateDTO updateDTO
    ) {
        log.info("PUT /api/v1/documentos/{} - Actualizando metadatos del documento", id);

        try {
            DocumentoDTO result = documentoService.updateDocumento(id, updateDTO);
            log.info("Documento actualizado exitosamente ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar documento ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Documento no encontrado para actualizar ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al actualizar documento ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * RF30 — Cambiar estado del documento en el flujo de aprobación.
     *
     * Operación dedicada para avanzar el documento en el workflow.
     * La validación de secuencia del flujo se realiza en el FlujoTrabajoTareaService.
     */
    @PatchMapping("/{id}/estado/{estadoId}")
    @Operation(
            summary = "Cambiar estado del documento",
            description = "Avanza el documento a un nuevo estado en el flujo de aprobación. " +
                    "Estados posibles: CREADO, EN_REVISION, APROBADO, RECHAZADO. RF30."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado cambiado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DocumentoDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Documento o estado no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición de estado no válida")
    })
    public ResponseEntity<DocumentoDTO> cambiarEstado(
            @Parameter(description = "ID del documento", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID del nuevo estado", required = true, example = "2")
            @PathVariable Long estadoId
    ) {
        log.info("PATCH /api/v1/documentos/{}/estado/{} - Cambiando estado del documento", id, estadoId);

        try {
            DocumentoDTO result = documentoService.cambiarEstado(id, estadoId);
            log.info("Estado del documento ID {} cambiado al estado ID {}", id, estadoId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Transición de estado no válida para documento ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.warn("Recurso no encontrado al cambiar estado documento ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    /**
     * RF20 — Eliminar documento.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar documento",
            description = "Elimina un documento del sistema. RF20."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Documento eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar: tiene tareas activas"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> deleteDocumento(
            @Parameter(description = "ID del documento a eliminar", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("DELETE /api/v1/documentos/{} - Eliminando documento", id);

        try {
            documentoService.deleteDocumento(id);
            log.info("Documento eliminado exitosamente ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("No se puede eliminar documento ID {} con tareas activas: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Documento no encontrado para eliminar ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al eliminar documento ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.AuditRegistroCreateDTO;
import com.eam.proyecto.businessLayer.dto.AuditRegistroDTO;
import com.eam.proyecto.businessLayer.service.AuditRegistroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para trazabilidad y auditoría de documentos.
 *
 * NOTA: En la arquitectura completa, los registros de auditoría se crean
 * AUTOMÁTICAMENTE en los servicios de negocio (DocumentoService, FlujoTrabajoTareaService, etc.)
 * Este endpoint permite el registro manual y la consulta del historial.
 *
 * ENDPOINTS:
 * - POST /api/v1/auditoria                                  → Registrar acción manual (RF33)
 * - GET  /api/v1/auditoria/{id}                             → Obtener registro por ID
 * - GET  /api/v1/auditoria/documento/{documentoId}          → Historial de un documento (RF34 / RF36)
 * - GET  /api/v1/auditoria/usuario/{cedula}                 → Trazabilidad por usuario (RF35)
 * - GET  /api/v1/auditoria/buscar                           → Consulta filtrada completa (RF36)
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auditoría", description = "Trazabilidad y registro de acciones sobre documentos — RF33, RF34, RF35, RF36")
@CrossOrigin(origins = "*")
public class AuditRegistroController {

    private final AuditRegistroService auditRegistroService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * RF33 — Registrar una acción de auditoría manualmente.
     *
     * NOTA: Normalmente los registros se crean automáticamente desde los servicios.
     * Este endpoint es útil para integraciones externas o acciones especiales.
     */
    @PostMapping
    @Operation(
            summary = "Registrar acción de auditoría",
            description = "Crea un registro de auditoría indicando quién hizo qué acción sobre qué documento. " +
                    "Normalmente este registro se crea automáticamente por el sistema. RF33."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Registro de auditoría creado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuditRegistroDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<AuditRegistroDTO> registrarAccion(
            @Parameter(description = "Datos del registro de auditoría", required = true)
            @RequestBody AuditRegistroCreateDTO createDTO
    ) {
        log.info("POST /api/v1/auditoria - Registrando acción de auditoría");

        try {
            AuditRegistroDTO result = auditRegistroService.registrarAccion(createDTO);
            log.info("Registro de auditoría creado exitosamente con ID: {}", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al registrar auditoría: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error al registrar auditoría: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Obtener registro de auditoría por ID.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener registro de auditoría por ID",
            description = "Retorna la información de un registro de auditoría específico."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registro encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuditRegistroDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Registro no encontrado")
    })
    public ResponseEntity<AuditRegistroDTO> getRegistroById(
            @Parameter(description = "ID del registro de auditoría", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/auditoria/{} - Buscando registro de auditoría", id);

        try {
            AuditRegistroDTO registro = auditRegistroService.getRegistroById(id);
            return ResponseEntity.ok(registro);
        } catch (RuntimeException e) {
            log.warn("Registro de auditoría no encontrado con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF34 / RF36 — Obtener historial completo de acciones sobre un documento.
     * Muestra todas las acciones realizadas: creación, cambios de estado, aprobaciones, etc.
     */
    @GetMapping("/documento/{documentoId}")
    @Operation(
            summary = "Historial de auditoría de un documento",
            description = "Retorna el historial completo de acciones realizadas sobre el documento indicado, " +
                    "ordenado cronológicamente. RF34 / RF36."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Historial del documento",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuditRegistroDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado")
    })
    public ResponseEntity<List<AuditRegistroDTO>> getHistorialByDocumento(
            @Parameter(description = "ID del documento", required = true, example = "1")
            @PathVariable Long documentoId
    ) {
        log.debug("GET /api/v1/auditoria/documento/{} - Obteniendo historial del documento", documentoId);

        try {
            List<AuditRegistroDTO> historial = auditRegistroService.getHistorialByDocumento(documentoId);
            log.debug("Se encontraron {} registros para el documento ID: {}", historial.size(), documentoId);
            return ResponseEntity.ok(historial);
        } catch (RuntimeException e) {
            log.warn("Documento no encontrado ID: {}", documentoId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF35 — Consultar trazabilidad de acciones realizadas por un usuario.
     * ¿Qué ha hecho este usuario en el sistema?
     */
    @GetMapping("/usuario/{cedula}")
    @Operation(
            summary = "Trazabilidad de un usuario",
            description = "Retorna todas las acciones realizadas por el usuario indicado sobre documentos. " +
                    "Registra quién hizo qué y cuándo. RF35."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Trazabilidad del usuario",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuditRegistroDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<List<AuditRegistroDTO>> getTrazabilidadByUsuario(
            @Parameter(description = "Cédula del usuario", required = true, example = "1234567890")
            @PathVariable Long cedula
    ) {
        log.debug("GET /api/v1/auditoria/usuario/{} - Obteniendo trazabilidad del usuario", cedula);

        try {
            List<AuditRegistroDTO> trazabilidad = auditRegistroService.getTrazabilidadByUsuario(cedula);
            log.debug("Se encontraron {} registros para el usuario cédula: {}", trazabilidad.size(), cedula);
            return ResponseEntity.ok(trazabilidad);
        } catch (RuntimeException e) {
            log.warn("Usuario no encontrado cédula: {}", cedula);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF36 — Consulta filtrada de trazabilidad completa.
     * Permite filtrar por tipo de acción y rango de fechas.
     */
    @GetMapping("/buscar")
    @Operation(
            summary = "Consulta filtrada de auditoría",
            description = "Permite consultar el registro de auditoría aplicando filtros por acción y rango de fechas. " +
                    "Todos los parámetros son opcionales. RF36."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Resultados de la consulta de auditoría",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AuditRegistroDTO.class))
    )
    public ResponseEntity<List<AuditRegistroDTO>> getTrazabilidadCompleta(
            @Parameter(description = "Tipo de acción a filtrar (ej: DOCUMENTO_CREADO, ESTADO_CAMBIADO, etc.)")
            @RequestParam(required = false) String accion,
            @Parameter(description = "Fecha de inicio del rango (ISO 8601: yyyy-MM-ddTHH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @Parameter(description = "Fecha de fin del rango (ISO 8601: yyyy-MM-ddTHH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        log.debug("GET /api/v1/auditoria/buscar - Consultando trazabilidad con filtros: accion={}, desde={}, hasta={}",
                accion, desde, hasta);

        List<AuditRegistroDTO> registros = auditRegistroService.getTrazabilidadCompleta(accion, desde, hasta);
        log.debug("Se encontraron {} registros en la consulta filtrada", registros.size());
        return ResponseEntity.ok(registros);
    }
}
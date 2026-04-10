package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.NotificacionCreateDTO;
import com.eam.proyecto.businessLayer.dto.NotificacionDTO;
import com.eam.proyecto.businessLayer.service.NotificacionService;
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
import java.util.Map;

/**
 * Controlador REST para gestión de notificaciones.
 *
 * ENDPOINTS:
 * - POST   /api/v1/notificaciones                          → Enviar notificación (RF37-RF39)
 * - GET    /api/v1/notificaciones/{id}                     → Obtener por ID
 * - GET    /api/v1/notificaciones/usuario/{cedula}         → Listar notificaciones del usuario
 * - GET    /api/v1/notificaciones/usuario/{cedula}/no-leidas → Listar no leídas (RF39)
 * - GET    /api/v1/notificaciones/usuario/{cedula}/contador → Contar no leídas
 * - PATCH  /api/v1/notificaciones/{id}/leida              → Marcar como leída
 * - PATCH  /api/v1/notificaciones/usuario/{cedula}/leer-todo → Marcar todas como leídas
 * - DELETE /api/v1/notificaciones/{id}                     → Eliminar notificación
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notificaciones", description = "Envío y gestión de notificaciones a usuarios — RF37, RF38, RF39")
@CrossOrigin(origins = "*")
public class NotificacionController {

    private final NotificacionService notificacionService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * RF37 / RF38 / RF39 — Enviar notificación a un usuario.
     *
     * NOTA: En la arquitectura completa, las notificaciones se envían automáticamente
     * desde los servicios de negocio ante eventos (creación, aprobación, asignación).
     * Este endpoint permite el envío manual o desde procesos externos.
     */
    @PostMapping
    @Operation(
            summary = "Enviar notificación",
            description = "Envía una notificación a un usuario del sistema. " +
                    "Puede ser por EMAIL o como notificación del SISTEMA. RF37 / RF38 / RF39."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Notificación enviada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NotificacionDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario destinatario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<NotificacionDTO> enviarNotificacion(
            @Parameter(description = "Datos de la notificación a enviar", required = true)
            @RequestBody NotificacionCreateDTO createDTO
    ) {
        log.info("POST /api/v1/notificaciones - Enviando notificación al usuario cédula: {}", createDTO.getDestinatarioCedula());

        try {
            NotificacionDTO result = notificacionService.enviarNotificacion(createDTO);
            log.info("Notificación enviada exitosamente con ID: {}", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al enviar notificación: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Usuario destinatario no encontrado: {}", e.getMessage());
                return ResponseEntity.notFound().build();
            }
            log.error("Error al enviar notificación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Obtener notificación por ID.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener notificación por ID",
            description = "Retorna la información completa de una notificación específica."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notificación encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NotificacionDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    })
    public ResponseEntity<NotificacionDTO> getNotificacionById(
            @Parameter(description = "ID de la notificación", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/notificaciones/{} - Buscando notificación", id);

        try {
            NotificacionDTO notificacion = notificacionService.getNotificacionById(id);
            return ResponseEntity.ok(notificacion);
        } catch (RuntimeException e) {
            log.warn("Notificación no encontrada con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todas las notificaciones de un usuario (leídas y no leídas).
     */
    @GetMapping("/usuario/{cedula}")
    @Operation(
            summary = "Listar notificaciones de un usuario",
            description = "Retorna todas las notificaciones (leídas y no leídas) del usuario indicado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de notificaciones"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<List<NotificacionDTO>> getNotificacionesByUsuario(
            @Parameter(description = "Cédula del usuario", required = true, example = "1234567890")
            @PathVariable Long cedula
    ) {
        log.debug("GET /api/v1/notificaciones/usuario/{} - Listando notificaciones del usuario", cedula);

        try {
            List<NotificacionDTO> notificaciones = notificacionService.getNotificacionesByUsuario(cedula);
            return ResponseEntity.ok(notificaciones);
        } catch (RuntimeException e) {
            log.warn("Usuario no encontrado cédula: {}", cedula);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF39 — Listar notificaciones no leídas de un usuario.
     * Usado para la campana de notificaciones en el dashboard.
     */
    @GetMapping("/usuario/{cedula}/no-leidas")
    @Operation(
            summary = "Listar notificaciones no leídas",
            description = "Retorna las notificaciones pendientes de leer del usuario. " +
                    "Usado para el indicador de notificaciones en el dashboard. RF39."
    )
    @ApiResponse(responseCode = "200", description = "Lista de notificaciones no leídas")
    public ResponseEntity<List<NotificacionDTO>> getNoLeidasByUsuario(
            @Parameter(description = "Cédula del usuario", required = true, example = "1234567890")
            @PathVariable Long cedula
    ) {
        log.debug("GET /api/v1/notificaciones/usuario/{}/no-leidas - Listando no leídas", cedula);

        try {
            List<NotificacionDTO> noLeidas = notificacionService.getNoLeidasByUsuario(cedula);
            return ResponseEntity.ok(noLeidas);
        } catch (RuntimeException e) {
            log.warn("Usuario no encontrado cédula: {}", cedula);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Contar notificaciones no leídas de un usuario.
     * Usado para el badge/contador de la campana de notificaciones.
     */
    @GetMapping("/usuario/{cedula}/contador")
    @Operation(
            summary = "Contar notificaciones no leídas",
            description = "Retorna el número de notificaciones no leídas del usuario. " +
                    "Usado para el badge/indicador numérico en el frontend."
    )
    @ApiResponse(responseCode = "200", description = "Contador de no leídas")
    public ResponseEntity<Map<String, Long>> countNoLeidasByUsuario(
            @Parameter(description = "Cédula del usuario", required = true, example = "1234567890")
            @PathVariable Long cedula
    ) {
        log.debug("GET /api/v1/notificaciones/usuario/{}/contador - Contando no leídas", cedula);

        long count = notificacionService.countNoLeidasByUsuario(cedula);
        return ResponseEntity.ok(Map.of("noLeidas", count));
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    /**
     * Marcar una notificación específica como leída.
     */
    @PatchMapping("/{id}/leida")
    @Operation(
            summary = "Marcar notificación como leída",
            description = "Marca una notificación específica como leída por el usuario destinatario."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notificación marcada como leída",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NotificacionDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    })
    public ResponseEntity<NotificacionDTO> marcarLeida(
            @Parameter(description = "ID de la notificación", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("PATCH /api/v1/notificaciones/{}/leida - Marcando como leída", id);

        try {
            NotificacionDTO result = notificacionService.marcarLeida(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.warn("Notificación no encontrada para marcar como leída ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Marcar todas las notificaciones de un usuario como leídas.
     */
    @PatchMapping("/usuario/{cedula}/leer-todo")
    @Operation(
            summary = "Marcar todas las notificaciones como leídas",
            description = "Marca todas las notificaciones no leídas del usuario como leídas de forma masiva."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Todas las notificaciones marcadas como leídas"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<Void> marcarTodasComoLeidas(
            @Parameter(description = "Cédula del usuario", required = true, example = "1234567890")
            @PathVariable Long cedula
    ) {
        log.info("PATCH /api/v1/notificaciones/usuario/{}/leer-todo - Marcando todas como leídas", cedula);

        try {
            notificacionService.marcarTodasComoLeidas(cedula);
            log.info("Todas las notificaciones marcadas como leídas para usuario cédula: {}", cedula);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.warn("Usuario no encontrado cédula: {}", cedula);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    /**
     * Eliminar notificación.
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar notificación",
            description = "Elimina físicamente una notificación del sistema."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Notificación eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> deleteNotificacion(
            @Parameter(description = "ID de la notificación", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.info("DELETE /api/v1/notificaciones/{} - Eliminando notificación", id);

        try {
            notificacionService.deleteNotificacion(id);
            log.info("Notificación eliminada exitosamente ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrada")) {
                log.warn("Notificación no encontrada para eliminar ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al eliminar notificación ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
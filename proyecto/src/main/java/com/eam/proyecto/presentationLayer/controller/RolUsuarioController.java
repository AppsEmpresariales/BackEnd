package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.RolUsuarioAsignarDTO;
import com.eam.proyecto.businessLayer.dto.RolUsuarioDTO;
import com.eam.proyecto.businessLayer.service.RolUsuarioService;
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
 * Controlador REST para asignación y revocación de roles a usuarios.
 *
 * ENDPOINTS:
 * - POST   /api/v1/roles-usuarios            → Asignar rol a usuario (RF15)
 * - DELETE /api/v1/roles-usuarios/{cedula}/{rolId} → Revocar rol de usuario (RF15)
 * - GET    /api/v1/roles-usuarios/{cedula}   → Listar roles de un usuario (RF05)
 */
@RestController
@RequestMapping("/api/v1/roles-usuarios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Roles de Usuario", description = "Asignación y revocación de roles a usuarios — RF05, RF15")
@CrossOrigin(origins = "*")
public class RolUsuarioController {

    private final RolUsuarioService rolUsuarioService;

    /**
     * RF15 — Asignar rol a un usuario.
     */
    @PostMapping
    @Operation(
            summary = "Asignar rol a usuario",
            description = "Asigna un rol específico a un usuario de la organización. RF15."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Rol asignado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RolUsuarioDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o rol ya asignado"),
            @ApiResponse(responseCode = "404", description = "Usuario o rol no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<RolUsuarioDTO> asignarRol(
            @Parameter(description = "Datos de asignación: cédula del usuario y ID del rol", required = true)
            @RequestBody RolUsuarioAsignarDTO asignarDTO
    ) {
        log.info("POST /api/v1/roles-usuarios - Asignando rol");

        try {
            RolUsuarioDTO result = rolUsuarioService.asignarRol(asignarDTO);
            log.info("Rol asignado exitosamente");
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al asignar rol: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Usuario o rol no encontrado: {}", e.getMessage());
                return ResponseEntity.notFound().build();
            }
            log.error("Error al asignar rol: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * RF15 — Revocar rol de un usuario.
     */
    @DeleteMapping("/{cedula}/{rolId}")
    @Operation(
            summary = "Revocar rol de usuario",
            description = "Elimina la asignación de un rol específico de un usuario. RF15."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rol revocado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Asignación no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> revocarRol(
            @Parameter(description = "Cédula del usuario", required = true, example = "1234567890")
            @PathVariable Long cedula,
            @Parameter(description = "ID del rol a revocar", required = true, example = "1")
            @PathVariable Long rolId
    ) {
        log.info("DELETE /api/v1/roles-usuarios/{}/{} - Revocando rol", cedula, rolId);

        try {
            rolUsuarioService.revocarRol(cedula, rolId);
            log.info("Rol revocado exitosamente al usuario cédula: {}", cedula);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrad")) {
                log.warn("Asignación no encontrada cédula: {}, rolId: {}", cedula, rolId);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al revocar rol cédula {}: {}", cedula, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * RF05 / RF15 — Listar roles asignados a un usuario.
     */
    @GetMapping("/{cedula}")
    @Operation(
            summary = "Listar roles de un usuario",
            description = "Retorna todos los roles asignados a un usuario específico. RF05 / RF15."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de roles del usuario",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RolUsuarioDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<List<RolUsuarioDTO>> getRolesByUsuario(
            @Parameter(description = "Cédula del usuario", required = true, example = "1234567890")
            @PathVariable Long cedula
    ) {
        log.debug("GET /api/v1/roles-usuarios/{} - Listando roles del usuario", cedula);

        try {
            List<RolUsuarioDTO> roles = rolUsuarioService.getRolesByUsuario(cedula);
            return ResponseEntity.ok(roles);
        } catch (RuntimeException e) {
            log.warn("Usuario no encontrado con cédula: {}", cedula);
            return ResponseEntity.notFound().build();
        }
    }
}
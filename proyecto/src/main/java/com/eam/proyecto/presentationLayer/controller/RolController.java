package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.RolDTO;
import com.eam.proyecto.businessLayer.service.RolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para consulta de roles del sistema.
 *
 * Los roles son catálogos predefinidos (ADMIN, USER).
 * Su gestión es de solo lectura desde esta API; se crean en el seed de BD.
 *
 * ENDPOINTS:
 * - GET /api/v1/roles          → Listar todos los roles (RF05)
 * - GET /api/v1/roles/{id}     → Obtener rol por ID
 * - GET /api/v1/roles/nombre/{nombre} → Obtener rol por nombre
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Roles", description = "Catálogo de roles del sistema (ADMIN, USER) — RF05, RF06")
@CrossOrigin(origins = "*")
public class RolController {

    private final RolService rolService;

    /**
     * RF05 — Listar todos los roles disponibles.
     */
    @GetMapping
    @Operation(
            summary = "Listar todos los roles",
            description = "Retorna el catálogo completo de roles disponibles en el sistema (ADMIN, USER). RF05."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de roles obtenida exitosamente",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RolDTO.class))
    )
    public ResponseEntity<List<RolDTO>> getAllRoles() {
        log.debug("GET /api/v1/roles - Listando todos los roles");
        List<RolDTO> roles = rolService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Obtener rol por ID.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener rol por ID",
            description = "Retorna la información de un rol específico dado su ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rol encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RolDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<RolDTO> getRolById(
            @Parameter(description = "ID del rol", required = true, example = "1")
            @PathVariable Long id
    ) {
        log.debug("GET /api/v1/roles/{} - Buscando rol", id);

        try {
            RolDTO rol = rolService.getRolById(id);
            return ResponseEntity.ok(rol);
        } catch (RuntimeException e) {
            log.warn("Rol no encontrado con ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtener rol por nombre (ADMIN / USER).
     */
    @GetMapping("/nombre/{nombre}")
    @Operation(
            summary = "Obtener rol por nombre",
            description = "Retorna el rol correspondiente al nombre indicado (ej: ADMIN, USER)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol encontrado"),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<RolDTO> getRolByNombre(
            @Parameter(description = "Nombre del rol", required = true, example = "ADMIN")
            @PathVariable String nombre
    ) {
        log.debug("GET /api/v1/roles/nombre/{} - Buscando rol por nombre", nombre);

        try {
            RolDTO rol = rolService.getRolByNombre(nombre);
            return ResponseEntity.ok(rol);
        } catch (RuntimeException e) {
            log.warn("Rol no encontrado con nombre: {}", nombre);
            return ResponseEntity.notFound().build();
        }
    }
}
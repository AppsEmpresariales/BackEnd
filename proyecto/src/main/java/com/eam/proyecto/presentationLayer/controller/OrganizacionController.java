package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.OrganizacionCreateDTO;
import com.eam.proyecto.businessLayer.dto.OrganizacionDTO;
import com.eam.proyecto.businessLayer.dto.OrganizacionUpdateDTO;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
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
 * Controlador REST para gestión de organizaciones (tenants).
 *
 * ENDPOINTS:
 * - POST   /api/v1/organizaciones               → Registrar nueva organización (RF01 / RF08)
 * - GET    /api/v1/organizaciones               → Listar todas las organizaciones
 * - GET    /api/v1/organizaciones/activas       → Listar organizaciones activas
 * - GET    /api/v1/organizaciones/{nit}         → Obtener organización por NIT (RF10)
 * - PUT    /api/v1/organizaciones/{nit}         → Actualizar organización (RF11)
 * - DELETE /api/v1/organizaciones/{nit}         → Eliminar organización
 */
@RestController
@RequestMapping("/api/v1/organizaciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organizaciones", description = "Gestión de organizaciones (tenants) en la plataforma SaaS — RF01, RF08, RF11")
@CrossOrigin(origins = "*")
public class OrganizacionController {

    private final OrganizacionService organizacionService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * RF01 / RF08 — Registrar nueva organización (tenant).
     *
     * VALIDACIONES:
     * - NIT único en la plataforma
     * - Email único en la plataforma
     * - Nombre y email obligatorios
     */
    @PostMapping
    @Operation(
            summary = "Registrar nueva organización",
            description = "Crea una nueva organización (tenant) en la plataforma. El NIT y el email deben ser únicos. " +
                    "La organización se activa automáticamente al crear. RF01 / RF08."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Organización creada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrganizacionDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, NIT o email ya registrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<OrganizacionDTO> createOrganizacion(
            @Parameter(description = "Datos de la organización a registrar", required = true)
            @RequestBody OrganizacionCreateDTO createDTO
    ) {
        log.info("POST /api/v1/organizaciones - Registrando organización NIT: {}", createDTO.getNit());

        try {
            OrganizacionDTO result = organizacionService.createOrganizacion(createDTO);
            log.info("Organización creada exitosamente NIT: {}", result.getNit());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear organización: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error al crear organización: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Panel global — Listar todas las organizaciones.
     */
    @GetMapping
    @Operation(
            summary = "Listar todas las organizaciones",
            description = "Obtiene la lista completa de organizaciones registradas en la plataforma. " +
                    "Uso exclusivo para administración global del sistema."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista obtenida exitosamente",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = OrganizacionDTO.class))
    )
    public ResponseEntity<List<OrganizacionDTO>> getAllOrganizaciones() {
        log.debug("GET /api/v1/organizaciones - Listando todas las organizaciones");
        List<OrganizacionDTO> organizaciones = organizacionService.getAllOrganizaciones();
        log.debug("Se encontraron {} organizaciones", organizaciones.size());
        return ResponseEntity.ok(organizaciones);
    }

    /**
     * Listar solo organizaciones activas (operativas).
     */
    @GetMapping("/activas")
    @Operation(
            summary = "Listar organizaciones activas",
            description = "Obtiene únicamente las organizaciones con estado activo=true."
    )
    @ApiResponse(responseCode = "200", description = "Lista de organizaciones activas")
    public ResponseEntity<List<OrganizacionDTO>> getOrganizacionesActivas() {
        log.debug("GET /api/v1/organizaciones/activas - Listando organizaciones activas");
        List<OrganizacionDTO> activas = organizacionService.getOrganizacionesActivas();
        return ResponseEntity.ok(activas);
    }

    /**
     * RF10 — Obtener organización por NIT.
     * Base del aislamiento multi-tenant.
     */
    @GetMapping("/{nit}")
    @Operation(
            summary = "Obtener organización por NIT",
            description = "Busca y retorna la información completa de una organización dado su NIT. RF10."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Organización encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrganizacionDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Organización no encontrada")
    })
    public ResponseEntity<OrganizacionDTO> getOrganizacionByNit(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/organizaciones/{} - Buscando organización", nit);

        try {
            OrganizacionDTO organizacion = organizacionService.getOrganizacionByNit(nit);
            return ResponseEntity.ok(organizacion);
        } catch (RuntimeException e) {
            log.warn("Organización no encontrada con NIT: {}", nit);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    /**
     * RF11 — Actualizar datos de la organización.
     * El NIT es inmutable. Solo el administrador puede realizar esta acción.
     */
    @PutMapping("/{nit}")
    @Operation(
            summary = "Actualizar organización",
            description = "Actualiza los datos de una organización existente. El NIT no se puede modificar. " +
                    "Los campos null se ignoran (actualización parcial). RF11."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Organización actualizada exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrganizacionDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Organización no encontrada")
    })
    public ResponseEntity<OrganizacionDTO> updateOrganizacion(
            @Parameter(description = "NIT de la organización a actualizar", required = true, example = "900123456")
            @PathVariable Long nit,
            @Parameter(description = "Datos a actualizar (campos null se ignoran)", required = true)
            @RequestBody OrganizacionUpdateDTO updateDTO
    ) {
        log.info("PUT /api/v1/organizaciones/{} - Actualizando organización", nit);

        try {
            OrganizacionDTO result = organizacionService.updateOrganizacion(nit, updateDTO);
            log.info("Organización actualizada exitosamente NIT: {}", nit);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar organización NIT {}: {}", nit, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrada")) {
                log.warn("Organización no encontrada para actualizar NIT: {}", nit);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al actualizar organización NIT {}: {}", nit, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    /**
     * Eliminar organización por NIT.
     * NOTA: En producción se prefiere desactivar (active=false) mediante PUT.
     */
    @DeleteMapping("/{nit}")
    @Operation(
            summary = "Eliminar organización",
            description = "Elimina físicamente una organización del sistema. " +
                    "PRECAUCIÓN: En producción se recomienda desactivar en lugar de eliminar."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Organización eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Organización no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> deleteOrganizacion(
            @Parameter(description = "NIT de la organización a eliminar", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.info("DELETE /api/v1/organizaciones/{} - Eliminando organización", nit);

        try {
            organizacionService.deleteOrganizacion(nit);
            log.info("Organización eliminada exitosamente NIT: {}", nit);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrada")) {
                log.warn("Organización no encontrada para eliminar NIT: {}", nit);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al eliminar organización NIT {}: {}", nit, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.dto.UsuarioCreateDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioUpdateDTO;
import com.eam.proyecto.businessLayer.service.UsuarioService;
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
 * Controlador REST para gestión de usuarios.
 *
 * ENDPOINTS:
 * - POST   /api/v1/usuarios                                   → Crear usuario (RF12)
 * - GET    /api/v1/usuarios/{cedula}                          → Obtener usuario por cédula
 * - GET    /api/v1/usuarios/organizacion/{nit}                → Listar usuarios por organización (RF16)
 * - GET    /api/v1/usuarios/organizacion/{nit}/activos        → Listar activos por organización (RF16)
 * - PUT    /api/v1/usuarios/{cedula}                          → Actualizar usuario (RF13)
 * - PATCH  /api/v1/usuarios/{cedula}/activar                  → Activar usuario (RF14)
 * - PATCH  /api/v1/usuarios/{cedula}/inactivar               → Inactivar usuario (RF14)
 * - DELETE /api/v1/usuarios/{cedula}                          → Eliminar usuario
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Usuarios", description = "Gestión de usuarios por organización — RF12, RF13, RF14, RF15, RF16")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * RF12 — Crear nuevo usuario dentro de una organización.
     *
     * FLUJO:
     * 1. Validar datos obligatorios
     * 2. Verificar organización activa — RF09
     * 3. Verificar email único en el tenant — RF10
     * 4. Encriptar contraseña con BCrypt
     */
    @PostMapping
    @Operation(
            summary = "Crear nuevo usuario",
            description = "Crea un usuario dentro de una organización. El email debe ser único. " +
                    "La contraseña se encripta automáticamente. RF12 / RF09."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuario creado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o email duplicado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<UsuarioDTO> createUsuario(
            @Parameter(description = "Datos del usuario a crear", required = true)
            @RequestBody UsuarioCreateDTO createDTO
    ) {
        log.info("POST /api/v1/usuarios - Creando usuario con cédula: {}", createDTO.getCedula());

        try {
            UsuarioDTO result = usuarioService.createUsuario(createDTO);
            log.info("Usuario creado exitosamente cédula: {}", result.getCedula());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear usuario: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error al crear usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    /**
     * Obtener usuario por cédula.
     */
    @GetMapping("/{cedula}")
    @Operation(
            summary = "Obtener usuario por cédula",
            description = "Retorna la información completa de un usuario dado su número de cédula."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UsuarioDTO> getUsuarioByCedula(
            @Parameter(description = "Cédula del usuario", required = true, example = "1234567890")
            @PathVariable Long cedula
    ) {
        log.debug("GET /api/v1/usuarios/{} - Buscando usuario", cedula);

        try {
            UsuarioDTO usuario = usuarioService.getUsuarioByCedula(cedula);
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e) {
            log.warn("Usuario no encontrado con cédula: {}", cedula);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF16 — Listar todos los usuarios de una organización.
     * Incluye activos e inactivos. Para administradores.
     */
    @GetMapping("/organizacion/{nit}")
    @Operation(
            summary = "Listar usuarios por organización",
            description = "Retorna todos los usuarios (activos e inactivos) pertenecientes a la organización indicada. RF16."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida"),
            @ApiResponse(responseCode = "404", description = "Organización no encontrada")
    })
    public ResponseEntity<List<UsuarioDTO>> getUsuariosByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/usuarios/organizacion/{} - Listando usuarios", nit);

        try {
            List<UsuarioDTO> usuarios = usuarioService.getUsuariosByOrganizacion(nit);
            log.debug("Se encontraron {} usuarios para la organización NIT: {}", usuarios.size(), nit);
            return ResponseEntity.ok(usuarios);
        } catch (RuntimeException e) {
            log.warn("Organización no encontrada NIT: {}", nit);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF16 — Listar solo usuarios activos de una organización.
     * Vista operativa para asignación de tareas.
     */
    @GetMapping("/organizacion/{nit}/activos")
    @Operation(
            summary = "Listar usuarios activos por organización",
            description = "Retorna únicamente los usuarios con active=true de la organización. " +
                    "Usado para asignación de tareas y flujos de trabajo. RF16."
    )
    @ApiResponse(responseCode = "200", description = "Lista de usuarios activos")
    public ResponseEntity<List<UsuarioDTO>> getUsuariosActivosByOrganizacion(
            @Parameter(description = "NIT de la organización", required = true, example = "900123456")
            @PathVariable Long nit
    ) {
        log.debug("GET /api/v1/usuarios/organizacion/{}/activos - Listando usuarios activos", nit);

        try {
            List<UsuarioDTO> activos = usuarioService.getUsuariosActivosByOrganizacion(nit);
            return ResponseEntity.ok(activos);
        } catch (RuntimeException e) {
            log.warn("Organización no encontrada NIT: {}", nit);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    /**
     * RF13 — Editar datos del usuario.
     * Cédula y organización son inmutables.
     */
    @PutMapping("/{cedula}")
    @Operation(
            summary = "Actualizar usuario",
            description = "Actualiza los datos de un usuario existente. La cédula y la organización no se pueden modificar. " +
                    "Los campos null se ignoran. RF13."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UsuarioDTO> updateUsuario(
            @Parameter(description = "Cédula del usuario a actualizar", required = true, example = "1234567890")
            @PathVariable Long cedula,
            @Parameter(description = "Datos a actualizar (campos null se ignoran)", required = true)
            @RequestBody UsuarioUpdateDTO updateDTO
    ) {
        log.info("PUT /api/v1/usuarios/{} - Actualizando usuario", cedula);

        try {
            UsuarioDTO result = usuarioService.updateUsuario(cedula, updateDTO);
            log.info("Usuario actualizado exitosamente cédula: {}", cedula);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar usuario cédula {}: {}", cedula, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Usuario no encontrado para actualizar cédula: {}", cedula);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al actualizar usuario cédula {}: {}", cedula, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * RF14 — Activar cuenta de usuario.
     * Solo aplicable a usuarios inactivos.
     */
    @PatchMapping("/{cedula}/activar")
    @Operation(
            summary = "Activar usuario",
            description = "Reactiva la cuenta de un usuario previamente inactivado. RF14."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario activado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "409", description = "El usuario ya está activo")
    })
    public ResponseEntity<UsuarioDTO> activarUsuario(
            @Parameter(description = "Cédula del usuario a activar", required = true, example = "1234567890")
            @PathVariable Long cedula
    ) {
        log.info("PATCH /api/v1/usuarios/{}/activar - Activando usuario", cedula);

        try {
            UsuarioDTO result = usuarioService.activarUsuario(cedula);
            log.info("Usuario activado exitosamente cédula: {}", cedula);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Conflicto al activar usuario cédula {}: {}", cedula, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.warn("Usuario no encontrado para activar cédula: {}", cedula);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * RF14 — Inactivar cuenta de usuario.
     * Solo aplicable a usuarios activos.
     */
    @PatchMapping("/{cedula}/inactivar")
    @Operation(
            summary = "Inactivar usuario",
            description = "Desactiva la cuenta de un usuario activo. El usuario no podrá iniciar sesión. RF14."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario inactivado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioDTO.class))
            ),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "409", description = "El usuario ya está inactivo")
    })
    public ResponseEntity<UsuarioDTO> inactivarUsuario(
            @Parameter(description = "Cédula del usuario a inactivar", required = true, example = "1234567890")
            @PathVariable Long cedula
    ) {
        log.info("PATCH /api/v1/usuarios/{}/inactivar - Inactivando usuario", cedula);

        try {
            UsuarioDTO result = usuarioService.inactivarUsuario(cedula);
            log.info("Usuario inactivado exitosamente cédula: {}", cedula);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            log.warn("Conflicto al inactivar usuario cédula {}: {}", cedula, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.warn("Usuario no encontrado para inactivar cédula: {}", cedula);
            return ResponseEntity.notFound().build();
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    /**
     * Eliminar usuario físicamente.
     * NOTA: En producción se prefiere inactivar — usar con precaución.
     */
    @DeleteMapping("/{cedula}")
    @Operation(
            summary = "Eliminar usuario",
            description = "Elimina físicamente un usuario del sistema. " +
                    "PRECAUCIÓN: En producción se recomienda inactivar en lugar de eliminar."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> deleteUsuario(
            @Parameter(description = "Cédula del usuario a eliminar", required = true, example = "1234567890")
            @PathVariable Long cedula
    ) {
        log.info("DELETE /api/v1/usuarios/{} - Eliminando usuario", cedula);

        try {
            usuarioService.deleteUsuario(cedula);
            log.info("Usuario eliminado exitosamente cédula: {}", cedula);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("no encontrado")) {
                log.warn("Usuario no encontrado para eliminar cédula: {}", cedula);
                return ResponseEntity.notFound().build();
            }
            log.error("Error al eliminar usuario cédula {}: {}", cedula, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
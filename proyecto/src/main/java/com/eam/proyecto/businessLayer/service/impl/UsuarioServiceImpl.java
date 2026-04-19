// UsuarioServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.UsuarioCreateDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioUpdateDTO;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.businessLayer.service.UsuarioService;
import com.eam.proyecto.persistenceLayer.dao.UsuarioDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final OrganizacionService organizacionService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * CREATE — Crear nuevo usuario dentro de una organización.
     *
     * FLUJO:
     * 1. Validar datos obligatorios
     * 2. Verificar que la organización existe y está activa — RF09
     * 3. Verificar unicidad de email dentro del tenant — RF10
     * 4. Encriptar contraseña con BCrypt
     * 5. Asignar active=true
     * 6. Persistir usando DAO
     *
     * RF12 / RF09
     */
    @Override
    public UsuarioDTO createUsuario(UsuarioCreateDTO createDTO) {
        log.info("Creando usuario con cédula: {}", createDTO.getCedula());

        // 1. Validaciones de negocio
        validateUsuarioData(createDTO);

        // 2. Verificar que la organización existe y está activa
        organizacionService.getOrganizacionActivaByNit(createDTO.getOrganizacionNit());

        // 3. Verificar unicidad de email en el tenant — RF10
        if (usuarioDAO.existsByEmail(createDTO.getEmail())) {
            log.warn("Intento de crear usuario con email duplicado: {}", createDTO.getEmail());
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + createDTO.getEmail());
        }

        // 4. Encriptar contraseña antes de persistir — SEGURIDAD CRÍTICA
        String passwordHash = passwordEncoder.encode(createDTO.getPassword());
        createDTO.setPasswordHash(passwordHash);
        createDTO.setActive(true);
        createDTO.setCreadoEn(LocalDateTime.now());

        UsuarioDTO result = usuarioDAO.save(createDTO);

        log.info("Usuario creado exitosamente con cédula: {}", result.getCedula());
        return result;
    }

    /**
     * READ — Buscar usuario por cédula.
     */
    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO getUsuarioByCedula(Long cedula) {
        log.debug("Buscando usuario por cédula: {}", cedula);

        return usuarioDAO.findByCedula(cedula)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con cédula: {}", cedula);
                    return new RuntimeException("Usuario no encontrado con cédula: " + cedula);
                });
    }

    /**
     * READ — Buscar usuario activo por email.
     * Usado en autenticación — RF02.
     *
     * SEGURIDAD: No se indica si el correo existe o no (mensaje genérico).
     */
    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO getUsuarioByEmail(String email) {
        log.debug("Buscando usuario activo por email");

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }

        return usuarioDAO.findByEmailAndActive(email)
                .orElseThrow(() -> {
                    log.warn("Usuario activo no encontrado para el email proporcionado");
                    return new RuntimeException("Credenciales inválidas");
                });
    }

    /**
     * READ ALL — Listar todos los usuarios de una organización — RF16.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO> getUsuariosByOrganizacion(Long organizacionNit) {
        log.debug("Obteniendo usuarios de la organización NIT: {}", organizacionNit);
        organizacionService.getOrganizacionByNit(organizacionNit);
        return usuarioDAO.findByOrganizacionNit(organizacionNit);
    }

    /**
     * READ ACTIVOS — Listar solo usuarios activos de una organización.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO> getUsuariosActivosByOrganizacion(Long organizacionNit) {
        log.debug("Obteniendo usuarios activos de la organización NIT: {}", organizacionNit);
        organizacionService.getOrganizacionByNit(organizacionNit);
        return usuarioDAO.findActivosByOrganizacionNit(organizacionNit);
    }

    /**
     * UPDATE — Editar datos del usuario — RF13.
     *
     * REGLAS:
     * - Cédula y organización son inmutables.
     * - Si se envía nueva contraseña, se encripta antes de persistir.
     */
    @Override
    public UsuarioDTO updateUsuario(Long cedula, UsuarioUpdateDTO updateDTO) {
        log.info("Actualizando usuario cédula: {}", cedula);

        getUsuarioByCedula(cedula);
        validateUsuarioUpdateData(updateDTO);

        // Si se actualiza contraseña, encriptar primero
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().trim().isEmpty()) {
            updateDTO.setPasswordHash(passwordEncoder.encode(updateDTO.getPassword()));
        }

        UsuarioDTO result = usuarioDAO.update(cedula, updateDTO)
                .orElseThrow(() -> new RuntimeException("Error al actualizar usuario con cédula: " + cedula));

        log.info("Usuario actualizado exitosamente cédula: {}", cedula);
        return result;
    }

    /**
     * ACTIVAR — Reactivar cuenta de usuario — RF14.
     */
    @Override
    public UsuarioDTO activarUsuario(Long cedula) {
        log.info("Activando usuario cédula: {}", cedula);

        UsuarioDTO usuario = getUsuarioByCedula(cedula);

        if (Boolean.TRUE.equals(usuario.getActive())) {
            throw new IllegalStateException("El usuario ya se encuentra activo");
        }

        UsuarioUpdateDTO updateDTO = new UsuarioUpdateDTO();
        updateDTO.setActive(true);

        UsuarioDTO result = usuarioDAO.update(cedula, updateDTO)
                .orElseThrow(() -> new RuntimeException("Error al activar usuario con cédula: " + cedula));

        log.info("Usuario activado exitosamente cédula: {}", cedula);
        return result;
    }

    /**
     * INACTIVAR — Desactivar cuenta de usuario — RF14.
     */
    @Override
    public UsuarioDTO inactivarUsuario(Long cedula) {
        log.info("Inactivando usuario cédula: {}", cedula);

        UsuarioDTO usuario = getUsuarioByCedula(cedula);

        if (Boolean.FALSE.equals(usuario.getActive())) {
            throw new IllegalStateException("El usuario ya se encuentra inactivo");
        }

        UsuarioUpdateDTO updateDTO = new UsuarioUpdateDTO();
        updateDTO.setActive(false);

        UsuarioDTO result = usuarioDAO.update(cedula, updateDTO)
                .orElseThrow(() -> new RuntimeException("Error al inactivar usuario con cédula: " + cedula));

        log.info("Usuario inactivado exitosamente cédula: {}", cedula);
        return result;
    }

    /**
     * DELETE — Eliminar usuario físicamente.
     *
     * NOTA: En producción se prefiere inactivar — usar con precaución.
     */
    @Override
    public void deleteUsuario(Long cedula) {
        log.info("Eliminando usuario cédula: {}", cedula);

        getUsuarioByCedula(cedula);

        boolean deleted = usuarioDAO.deleteByCedula(cedula);
        if (!deleted) {
            throw new RuntimeException("Error al eliminar usuario con cédula: " + cedula);
        }

        log.info("Usuario eliminado exitosamente cédula: {}", cedula);
    }

    // ─── Validaciones privadas ────────────────────────────────────────────────

    private void validateUsuarioData(UsuarioCreateDTO dto) {
        if (dto.getCedula() == null || dto.getCedula() <= 0) {
            throw new IllegalArgumentException("La cédula debe ser un número positivo");
        }
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del usuario es obligatorio");
        }
        if (dto.getNombre().length() > 150) {
            throw new IllegalArgumentException("El nombre no puede exceder 150 caracteres");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        if (dto.getOrganizacionNit() == null) {
            throw new IllegalArgumentException("El NIT de la organización es obligatorio");
        }
    }

    private void validateUsuarioUpdateData(UsuarioUpdateDTO dto) {
        if (dto.getNombre() != null) {
            if (dto.getNombre().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre no puede estar vacío");
            }
            if (dto.getNombre().length() > 150) {
                throw new IllegalArgumentException("El nombre no puede exceder 150 caracteres");
            }
        }
        if (dto.getPassword() != null && dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
    }
}
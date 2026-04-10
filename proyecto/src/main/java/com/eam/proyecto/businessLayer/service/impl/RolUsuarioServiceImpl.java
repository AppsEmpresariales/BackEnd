// RolUsuarioServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.RolUsuarioAsignarDTO;
import com.eam.proyecto.businessLayer.dto.RolUsuarioDTO;
import com.eam.proyecto.businessLayer.service.RolService;
import com.eam.proyecto.businessLayer.service.RolUsuarioService;
import com.eam.proyecto.businessLayer.service.UsuarioService;
import com.eam.proyecto.persistenceLayer.dao.RolUsuarioDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RolUsuarioServiceImpl implements RolUsuarioService {

    private final RolUsuarioDAO rolUsuarioDAO;
    private final UsuarioService usuarioService;
    private final RolService rolService;

    /**
     * ASIGNAR ROL — Asignar un rol a un usuario — RF15.
     *
     * FLUJO:
     * 1. Verificar que el usuario existe
     * 2. Verificar que el rol existe en el catálogo
     * 3. Verificar que no exista ya la asignación (evitar duplicado)
     * 4. Persistir
     */
    @Override
    public RolUsuarioDTO asignarRol(RolUsuarioAsignarDTO asignarDTO) {
        log.info("Asignando rol ID {} al usuario cédula {}", asignarDTO.getRolId(), asignarDTO.getUsuarioCedula());

        if (asignarDTO.getUsuarioCedula() == null) {
            throw new IllegalArgumentException("La cédula del usuario es obligatoria");
        }
        if (asignarDTO.getRolId() == null) {
            throw new IllegalArgumentException("El ID del rol es obligatorio");
        }

        // Verificar que el usuario y el rol existen (lanzan excepción si no)
        usuarioService.getUsuarioByCedula(asignarDTO.getUsuarioCedula());
        rolService.getRolById(asignarDTO.getRolId());

        // Evitar asignación duplicada — constraint UNIQUE en BD
        if (rolUsuarioDAO.existeAsignacion(asignarDTO.getUsuarioCedula(), asignarDTO.getRolId())) {
            log.warn("El usuario {} ya tiene asignado el rol {}", asignarDTO.getUsuarioCedula(), asignarDTO.getRolId());
            throw new IllegalStateException("El usuario ya tiene asignado este rol");
        }

        RolUsuarioDTO result = rolUsuarioDAO.save(asignarDTO);

        log.info("Rol asignado exitosamente al usuario cédula: {}", asignarDTO.getUsuarioCedula());
        return result;
    }

    /**
     * REVOCAR ROL — Quitar un rol a un usuario — RF15.
     */
    @Override
    public void revocarRol(Long cedula, Long rolId) {
        log.info("Revocando rol ID {} del usuario cédula {}", rolId, cedula);

        usuarioService.getUsuarioByCedula(cedula);
        rolService.getRolById(rolId);

        if (!rolUsuarioDAO.existeAsignacion(cedula, rolId)) {
            log.warn("El usuario {} no tiene asignado el rol {}", cedula, rolId);
            throw new IllegalStateException("El usuario no tiene asignado el rol indicado");
        }

        rolUsuarioDAO.deleteByUsuarioCedulaAndRolId(cedula, rolId);

        log.info("Rol revocado exitosamente del usuario cédula: {}", cedula);
    }

    /**
     * READ — Obtener todos los roles de un usuario — RF05 / RF06.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RolUsuarioDTO> getRolesByUsuario(Long cedula) {
        log.debug("Obteniendo roles del usuario cédula: {}", cedula);
        usuarioService.getUsuarioByCedula(cedula);
        return rolUsuarioDAO.findByUsuarioCedula(cedula);
    }
}
// RolServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.RolDTO;
import com.eam.proyecto.businessLayer.service.RolService;
import com.eam.proyecto.persistenceLayer.dao.RolDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RolServiceImpl implements RolService {

    private final RolDAO rolDAO;

    /**
     * READ — Buscar rol por ID.
     *
     * CASO DE USO: Validar que el rol existe antes de asignarlo — RF15.
     */
    @Override
    public RolDTO getRolById(Long id) {
        log.debug("Buscando rol por ID: {}", id);

        return rolDAO.findById(id)
                .orElseThrow(() -> {
                    log.warn("Rol no encontrado con ID: {}", id);
                    return new RuntimeException("Rol no encontrado con ID: " + id);
                });
    }

    /**
     * READ — Buscar rol por nombre.
     *
     * CASO DE USO: Spring Security carga el rol por nombre para evaluar permisos — RF06.
     */
    @Override
    public RolDTO getRolByNombre(String nombre) {
        log.debug("Buscando rol por nombre: {}", nombre);

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }

        return rolDAO.findByNombre(nombre)
                .orElseThrow(() -> {
                    log.warn("Rol no encontrado con nombre: {}", nombre);
                    return new RuntimeException("Rol no encontrado con nombre: " + nombre);
                });
    }

    /**
     * READ ALL — Listar todos los roles disponibles — RF05.
     */
    @Override
    public List<RolDTO> getAllRoles() {
        log.debug("Obteniendo catálogo de roles");
        return rolDAO.findAll();
    }
}
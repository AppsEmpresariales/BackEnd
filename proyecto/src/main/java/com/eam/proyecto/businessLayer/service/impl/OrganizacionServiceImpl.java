// OrganizacionServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.OrganizacionCreateDTO;
import com.eam.proyecto.businessLayer.dto.OrganizacionDTO;
import com.eam.proyecto.businessLayer.dto.OrganizacionUpdateDTO;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.persistenceLayer.dao.OrganizacionDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrganizacionServiceImpl implements OrganizacionService {

    private final OrganizacionDAO organizacionDAO;

    /**
     * CREATE — Registrar nueva organización (tenant).
     *
     * FLUJO:
     * 1. Validar datos obligatorios
     * 2. Verificar unicidad de NIT y email
     * 3. Asignar active=true y creadoEn antes de persistir
     * 4. Delegar al DAO
     *
     * RF01 / RF08
     */
    @Override
    public OrganizacionDTO createOrganizacion(OrganizacionCreateDTO createDTO) {
        log.info("Creando organización con NIT: {}", createDTO.getNit());

        // 1. Validaciones de negocio
        validateOrganizacionData(createDTO);

        // 2. Verificar que el NIT no esté registrado — RF01 Escenario 2
        if (organizacionDAO.existsByNit(createDTO.getNit())) {
            log.warn("Intento de crear organización con NIT duplicado: {}", createDTO.getNit());
            throw new IllegalArgumentException("Ya existe una organización registrada con el NIT: " + createDTO.getNit());
        }

        // 2b. Verificar unicidad de email
        if (organizacionDAO.existsByEmail(createDTO.getEmail())) {
            log.warn("Intento de crear organización con email duplicado: {}", createDTO.getEmail());
            throw new IllegalArgumentException("Ya existe una organización registrada con el email: " + createDTO.getEmail());
        }

        // 3. El service asigna los campos gestionados internamente
        createDTO.setActive(true);
        createDTO.setCreadoEn(LocalDateTime.now());

        // 4. Persistir usando DAO
        OrganizacionDTO result = organizacionDAO.save(createDTO);

        log.info("Organización creada exitosamente con NIT: {}", result.getNit());
        return result;
    }

    /**
     * READ — Obtener organización por NIT (sin restricción de estado).
     *
     * RF10: Aislamiento lógico de datos entre organizaciones.
     */
    @Override
    @Transactional(readOnly = true)
    public OrganizacionDTO getOrganizacionByNit(Long nit) {
        log.debug("Buscando organización por NIT: {}", nit);

        return organizacionDAO.findByNit(nit)
                .orElseThrow(() -> {
                    log.warn("Organización no encontrada con NIT: {}", nit);
                    return new RuntimeException("Organización no encontrada con NIT: " + nit);
                });
    }

    /**
     * READ — Obtener organización activa por NIT.
     * Usado en autenticación y operaciones de negocio normales.
     */
    @Override
    @Transactional(readOnly = true)
    public OrganizacionDTO getOrganizacionActivaByNit(Long nit) {
        log.debug("Buscando organización activa por NIT: {}", nit);

        return organizacionDAO.findActivaByNit(nit)
                .orElseThrow(() -> {
                    log.warn("Organización inactiva o no encontrada con NIT: {}", nit);
                    return new RuntimeException("Organización no encontrada o inactiva con NIT: " + nit);
                });
    }

    /**
     * READ ALL — Listar todas las organizaciones (panel global de administración).
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrganizacionDTO> getAllOrganizaciones() {
        log.debug("Obteniendo todas las organizaciones");
        return organizacionDAO.findAll();
    }

    /**
     * READ ACTIVAS — Listar solo organizaciones operativas.
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrganizacionDTO> getOrganizacionesActivas() {
        log.debug("Obteniendo organizaciones activas");
        return organizacionDAO.findActivas();
    }

    /**
     * UPDATE — Actualizar datos de la organización.
     *
     * REGLA: NIT es inmutable. Solo el administrador puede actualizar — RF11.
     */
    @Override
    public OrganizacionDTO updateOrganizacion(Long nit, OrganizacionUpdateDTO updateDTO) {
        log.info("Actualizando organización NIT: {}", nit);

        // Verificar que existe
        getOrganizacionByNit(nit);

        // Validar datos de actualización
        validateOrganizacionUpdateData(updateDTO);

        OrganizacionDTO result = organizacionDAO.update(nit, updateDTO)
                .orElseThrow(() -> new RuntimeException("Error al actualizar organización con NIT: " + nit));

        log.info("Organización actualizada exitosamente NIT: {}", nit);
        return result;
    }

    /**
     * DELETE — Eliminar organización por NIT.
     *
     * NOTA: En producción se prefiere desactivar (active=false).
     * Validar que no tenga usuarios o documentos activos antes de eliminar.
     */
    @Override
    public void deleteOrganizacion(Long nit) {
        log.info("Eliminando organización NIT: {}", nit);

        getOrganizacionByNit(nit);

        boolean deleted = organizacionDAO.deleteByNit(nit);
        if (!deleted) {
            throw new RuntimeException("Error al eliminar organización con NIT: " + nit);
        }

        log.info("Organización eliminada exitosamente NIT: {}", nit);
    }

    // ─── Validaciones privadas ────────────────────────────────────────────────

    private void validateOrganizacionData(OrganizacionCreateDTO dto) {
        if (dto.getNit() == null || dto.getNit() <= 0) {
            throw new IllegalArgumentException("El NIT debe ser un número positivo");
        }
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la organización es obligatorio");
        }
        if (dto.getNombre().length() > 200) {
            throw new IllegalArgumentException("El nombre no puede exceder 200 caracteres");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email de la organización es obligatorio");
        }
        if (!isValidEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El formato del email no es válido");
        }
    }

    private void validateOrganizacionUpdateData(OrganizacionUpdateDTO dto) {
        if (dto.getNombre() != null) {
            if (dto.getNombre().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre no puede estar vacío");
            }
            if (dto.getNombre().length() > 200) {
                throw new IllegalArgumentException("El nombre no puede exceder 200 caracteres");
            }
        }
        if (dto.getEmail() != null && !isValidEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El formato del email no es válido");
        }
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") &&
                email.indexOf("@") < email.lastIndexOf(".") &&
                email.length() > 5;
    }
}
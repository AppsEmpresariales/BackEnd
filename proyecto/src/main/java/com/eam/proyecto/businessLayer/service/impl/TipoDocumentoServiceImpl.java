// TipoDocumentoServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.TipoDocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.TipoDocumentoDTO;
import com.eam.proyecto.businessLayer.dto.TipoDocumentoUpdateDTO;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.businessLayer.service.TipoDocumentoService;
import com.eam.proyecto.persistenceLayer.dao.TipoDocumentoDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TipoDocumentoServiceImpl implements TipoDocumentoService {

    private final TipoDocumentoDAO tipoDocumentoDAO;
    private final OrganizacionService organizacionService;

    /**
     * CREATE — Crear tipo documental para una organización — RF24 / RF42.
     *
     * FLUJO:
     * 1. Validar datos
     * 2. Verificar que la organización existe
     * 3. Verificar unicidad de nombre dentro del tenant — RF10
     * 4. Asignar active=true
     * 5. Persistir
     */
    @Override
    public TipoDocumentoDTO createTipoDocumento(TipoDocumentoCreateDTO createDTO) {
        log.info("Creando tipo documental '{}' para organización NIT: {}", createDTO.getNombre(), createDTO.getOrganizacionNit());

        validateTipoDocumentoData(createDTO);
        organizacionService.getOrganizacionActivaByNit(createDTO.getOrganizacionNit());

        // Verificar nombre único dentro del tenant
        if (tipoDocumentoDAO.existsByNombreAndOrganizacionNit(createDTO.getNombre(), createDTO.getOrganizacionNit())) {
            log.warn("Tipo documental duplicado '{}' en organización NIT: {}", createDTO.getNombre(), createDTO.getOrganizacionNit());
            throw new IllegalArgumentException("Ya existe un tipo documental con el nombre '" + createDTO.getNombre() + "' en esta organización");
        }

        createDTO.setActive(true);

        TipoDocumentoDTO result = tipoDocumentoDAO.save(createDTO);

        log.info("Tipo documental creado exitosamente con ID: {}", result.getId());
        return result;
    }

    /**
     * READ — Buscar tipo documental por ID (sin restricción de tenant).
     */
    @Override
    @Transactional(readOnly = true)
    public TipoDocumentoDTO getTipoDocumentoById(Long id) {
        log.debug("Buscando tipo documental por ID: {}", id);

        return tipoDocumentoDAO.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tipo documental no encontrado con ID: {}", id);
                    return new RuntimeException("Tipo documental no encontrado con ID: " + id);
                });
    }

    /**
     * READ — Buscar tipo documental por ID restringido al tenant — RF10.
     */
    @Override
    @Transactional(readOnly = true)
    public TipoDocumentoDTO getTipoDocumentoByIdAndOrganizacion(Long id, Long organizacionNit) {
        log.debug("Buscando tipo documental ID {} para organización NIT: {}", id, organizacionNit);

        return tipoDocumentoDAO.findByIdAndOrganizacionNit(id, organizacionNit)
                .orElseThrow(() -> {
                    log.warn("Tipo documental ID {} no encontrado en organización NIT: {}", id, organizacionNit);
                    return new RuntimeException("Tipo documental no encontrado en esta organización");
                });
    }

    /**
     * READ ACTIVOS — Tipos documentales activos de la organización — RF27.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TipoDocumentoDTO> getTiposActivosByOrganizacion(Long organizacionNit) {
        log.debug("Obteniendo tipos documentales activos para NIT: {}", organizacionNit);
        organizacionService.getOrganizacionByNit(organizacionNit);
        return tipoDocumentoDAO.findActivosByOrganizacionNit(organizacionNit);
    }

    /**
     * READ ALL — Todos los tipos de la organización (activos e inactivos) — RF42.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TipoDocumentoDTO> getAllTiposByOrganizacion(Long organizacionNit) {
        log.debug("Obteniendo todos los tipos documentales para NIT: {}", organizacionNit);
        organizacionService.getOrganizacionByNit(organizacionNit);
        return tipoDocumentoDAO.findAllByOrganizacionNit(organizacionNit);
    }

    /**
     * UPDATE — Editar nombre/descripción del tipo documental — RF25 / RF42.
     */
    @Override
    public TipoDocumentoDTO updateTipoDocumento(Long id, TipoDocumentoUpdateDTO updateDTO) {
        log.info("Actualizando tipo documental ID: {}", id);

        TipoDocumentoDTO existing = getTipoDocumentoById(id);
        validateTipoDocumentoUpdateData(updateDTO);

        // Si cambia el nombre, verificar unicidad en el mismo tenant
        if (updateDTO.getNombre() != null && !updateDTO.getNombre().equals(existing.getNombre())) {
            if (tipoDocumentoDAO.existsByNombreAndOrganizacionNit(updateDTO.getNombre(), existing.getOrganizacionNit())) {
                throw new IllegalArgumentException("Ya existe un tipo documental con el nombre '" + updateDTO.getNombre() + "' en esta organización");
            }
        }

        TipoDocumentoDTO result = tipoDocumentoDAO.update(id, updateDTO)
                .orElseThrow(() -> new RuntimeException("Error al actualizar tipo documental ID: " + id));

        log.info("Tipo documental actualizado exitosamente ID: {}", id);
        return result;
    }

    /**
     * DESACTIVAR — Eliminación lógica — RF26.
     *
     * POLÍTICA: No se elimina físicamente para preservar la integridad
     * de los documentos existentes que referencian este tipo.
     */
    @Override
    public TipoDocumentoDTO desactivarTipoDocumento(Long id) {
        log.info("Desactivando tipo documental ID: {}", id);

        TipoDocumentoDTO existing = getTipoDocumentoById(id);

        if (Boolean.FALSE.equals(existing.getActive())) {
            throw new IllegalStateException("El tipo documental ya se encuentra inactivo");
        }

        TipoDocumentoDTO result = tipoDocumentoDAO.desactivar(id)
                .orElseThrow(() -> new RuntimeException("Error al desactivar tipo documental ID: " + id));

        log.info("Tipo documental desactivado exitosamente ID: {}", id);
        return result;
    }

    /**
     * ACTIVAR — Reactivar tipo documental — RF42.
     */
    @Override
    public TipoDocumentoDTO activarTipoDocumento(Long id) {
        log.info("Activando tipo documental ID: {}", id);

        TipoDocumentoDTO existing = getTipoDocumentoById(id);

        if (Boolean.TRUE.equals(existing.getActive())) {
            throw new IllegalStateException("El tipo documental ya se encuentra activo");
        }

        TipoDocumentoDTO result = tipoDocumentoDAO.activar(id)
                .orElseThrow(() -> new RuntimeException("Error al activar tipo documental ID: " + id));

        log.info("Tipo documental activado exitosamente ID: {}", id);
        return result;
    }

    /**
     * DELETE — Eliminar físicamente — usar solo si no tiene documentos asociados.
     */
    @Override
    public void deleteTipoDocumento(Long id) {
        log.info("Eliminando tipo documental ID: {}", id);

        getTipoDocumentoById(id);

        boolean deleted = tipoDocumentoDAO.deleteById(id);
        if (!deleted) {
            throw new RuntimeException("Error al eliminar tipo documental ID: " + id);
        }

        log.info("Tipo documental eliminado exitosamente ID: {}", id);
    }

    // ─── Validaciones privadas ────────────────────────────────────────────────

    private void validateTipoDocumentoData(TipoDocumentoCreateDTO dto) {
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del tipo documental es obligatorio");
        }
        if (dto.getNombre().length() > 150) {
            throw new IllegalArgumentException("El nombre no puede exceder 150 caracteres");
        }
        if (dto.getOrganizacionNit() == null) {
            throw new IllegalArgumentException("El NIT de la organización es obligatorio");
        }
    }

    private void validateTipoDocumentoUpdateData(TipoDocumentoUpdateDTO dto) {
        if (dto.getNombre() != null) {
            if (dto.getNombre().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre no puede estar vacío");
            }
            if (dto.getNombre().length() > 150) {
                throw new IllegalArgumentException("El nombre no puede exceder 150 caracteres");
            }
        }
    }
}
// EstadoDocumentoServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.EstadoDocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.EstadoDocumentoDTO;
import com.eam.proyecto.businessLayer.dto.EstadoDocumentoUpdateDTO;
import com.eam.proyecto.businessLayer.service.EstadoDocumentoService;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.persistenceLayer.dao.EstadoDocumentoDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EstadoDocumentoServiceImpl implements EstadoDocumentoService {

    private final EstadoDocumentoDAO estadoDocumentoDAO;
    private final OrganizacionService organizacionService;

    /**
     * CREATE — Crear estado documental para una organización — RF41.
     *
     * FLUJO:
     * 1. Validar datos
     * 2. Verificar que la organización existe
     * 3. Verificar unicidad del nombre dentro del tenant
     * 4. REGLA CRÍTICA: solo puede haber un esInicial=true por organización
     * 5. Persistir
     */
    @Override
    public EstadoDocumentoDTO createEstadoDocumento(EstadoDocumentoCreateDTO createDTO) {
        log.info("Creando estado documental '{}' para organización NIT: {}", createDTO.getNombre(), createDTO.getOrganizacionNit());

        validateEstadoDocumentoData(createDTO);
        organizacionService.getOrganizacionActivaByNit(createDTO.getOrganizacionNit());

        // Verificar unicidad de nombre en el tenant
        if (estadoDocumentoDAO.findByNombreAndOrganizacionNit(createDTO.getNombre(), createDTO.getOrganizacionNit()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un estado con el nombre '" + createDTO.getNombre() + "' en esta organización");
        }

        // REGLA CRÍTICA: solo un estado inicial por organización — RF31
        boolean esInicial = Boolean.TRUE.equals(createDTO.getEsInicial());
        if (esInicial && estadoDocumentoDAO.findInicialByOrganizacionNit(createDTO.getOrganizacionNit()).isPresent()) {
            log.warn("Intento de crear segundo estado inicial en organización NIT: {}", createDTO.getOrganizacionNit());
            throw new IllegalStateException("Ya existe un estado inicial para esta organización. Desactívelo antes de crear uno nuevo");
        }

        EstadoDocumentoDTO result = estadoDocumentoDAO.save(createDTO);

        log.info("Estado documental creado exitosamente con ID: {}", result.getId());
        return result;
    }

    /**
     * READ — Buscar estado por ID.
     */
    @Override
    @Transactional(readOnly = true)
    public EstadoDocumentoDTO getEstadoDocumentoById(Long id) {
        log.debug("Buscando estado documental por ID: {}", id);

        return estadoDocumentoDAO.findById(id)
                .orElseThrow(() -> {
                    log.warn("Estado documental no encontrado con ID: {}", id);
                    return new RuntimeException("Estado documental no encontrado con ID: " + id);
                });
    }

    /**
     * READ INICIAL — Obtener el estado inicial de la organización.
     *
     * RF17: Se asigna automáticamente al crear un documento.
     * RF31: Validar secuencia del flujo.
     */
    @Override
    @Transactional(readOnly = true)
    public EstadoDocumentoDTO getEstadoInicialByOrganizacion(Long organizacionNit) {
        log.debug("Buscando estado inicial para organización NIT: {}", organizacionNit);

        return estadoDocumentoDAO.findInicialByOrganizacionNit(organizacionNit)
                .orElseThrow(() -> {
                    log.warn("Sin estado inicial configurado para organización NIT: {}", organizacionNit);
                    return new IllegalStateException("La organización no tiene un estado inicial configurado. Configure uno en RF41");
                });
    }

    /**
     * READ ALL — Listar todos los estados de la organización — RF41.
     */
    @Override
    @Transactional(readOnly = true)
    public List<EstadoDocumentoDTO> getEstadosByOrganizacion(Long organizacionNit) {
        log.debug("Obteniendo estados documentales para organización NIT: {}", organizacionNit);
        organizacionService.getOrganizacionByNit(organizacionNit);
        return estadoDocumentoDAO.findByOrganizacionNit(organizacionNit);
    }

    /**
     * READ FINALES — Listar estados finales de la organización — RF31.
     */
    @Override
    @Transactional(readOnly = true)
    public List<EstadoDocumentoDTO> getEstadosFinalesByOrganizacion(Long organizacionNit) {
        log.debug("Obteniendo estados finales para organización NIT: {}", organizacionNit);
        organizacionService.getOrganizacionByNit(organizacionNit);
        return estadoDocumentoDAO.findFinalesByOrganizacionNit(organizacionNit);
    }

    /**
     * UPDATE — Actualizar nombre/color del estado — RF41.
     */
    @Override
    public EstadoDocumentoDTO updateEstadoDocumento(Long id, EstadoDocumentoUpdateDTO updateDTO) {
        log.info("Actualizando estado documental ID: {}", id);

        getEstadoDocumentoById(id);

        EstadoDocumentoDTO result = estadoDocumentoDAO.update(id, updateDTO)
                .orElseThrow(() -> new RuntimeException("Error al actualizar estado documental ID: " + id));

        log.info("Estado documental actualizado exitosamente ID: {}", id);
        return result;
    }

    /**
     * DELETE — Eliminar estado.
     *
     * RESTRICCIÓN: El service debe validar que ningún documento
     * esté en este estado antes de eliminar.
     */
    @Override
    public void deleteEstadoDocumento(Long id) {
        log.info("Eliminando estado documental ID: {}", id);

        getEstadoDocumentoById(id);

        boolean deleted = estadoDocumentoDAO.deleteById(id);
        if (!deleted) {
            throw new RuntimeException("Error al eliminar estado documental ID: " + id);
        }

        log.info("Estado documental eliminado exitosamente ID: {}", id);
    }

    // ─── Validaciones privadas ────────────────────────────────────────────────

    private void validateEstadoDocumentoData(EstadoDocumentoCreateDTO dto) {
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del estado es obligatorio");
        }
        if (dto.getNombre().length() > 100) {
            throw new IllegalArgumentException("El nombre no puede exceder 100 caracteres");
        }
        if (dto.getOrganizacionNit() == null) {
            throw new IllegalArgumentException("El NIT de la organización es obligatorio");
        }
        if (dto.getColor() != null && !dto.getColor().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("El color debe ser un código hexadecimal válido (ej: #4CAF50)");
        }
    }
}
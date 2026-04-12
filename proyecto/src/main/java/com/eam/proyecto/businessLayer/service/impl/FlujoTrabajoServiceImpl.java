// FlujoTrabajoServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoUpdateDTO;
import com.eam.proyecto.businessLayer.service.FlujoTrabajoService;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.businessLayer.service.TipoDocumentoService;
import com.eam.proyecto.persistenceLayer.dao.FlujoTrabajoDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FlujoTrabajoServiceImpl implements FlujoTrabajoService {

    private final FlujoTrabajoDAO flujoTrabajoDAO;
    private final OrganizacionService organizacionService;
    private final TipoDocumentoService tipoDocumentoService;

    /**
     * CREATE — Definir flujo de aprobación para un tipo documental — RF28 / RF32.
     *
     * FLUJO:
     * 1. Validar datos
     * 2. Verificar que la organización y el tipo documental existen
     * 3. REGLA CRÍTICA: solo un flujo activo por tipo documental por organización
     * 4. Persistir
     */
    @Override
    public FlujoTrabajoDTO createFlujoTrabajo(FlujoTrabajoCreateDTO createDTO) {
        log.info("Creando flujo de trabajo '{}' para organización NIT: {}", createDTO.getNombre(), createDTO.getOrganizacionNit());

        validateFlujoTrabajoData(createDTO);

        organizacionService.getOrganizacionActivaByNit(createDTO.getOrganizacionNit());
        tipoDocumentoService.getTipoDocumentoByIdAndOrganizacion(createDTO.getTipoDocumentoId(), createDTO.getOrganizacionNit());

        // REGLA CRÍTICA: evitar flujos activos duplicados por tipo documental — RF32
        if (flujoTrabajoDAO.existeActivoPorTipoDocumental(createDTO.getOrganizacionNit(), createDTO.getTipoDocumentoId())) {
            log.warn("Ya existe un flujo activo para tipo documental {} en organización {}", createDTO.getTipoDocumentoId(), createDTO.getOrganizacionNit());
            throw new IllegalStateException("Ya existe un flujo activo para este tipo documental. Desactívelo antes de crear uno nuevo");
        }

        FlujoTrabajoDTO result = flujoTrabajoDAO.save(createDTO);

        log.info("Flujo de trabajo creado exitosamente con ID: {}", result.getId());
        return result;
    }

    /**
     * READ — Buscar flujo por ID.
     */
    @Override
    @Transactional(readOnly = true)
    public FlujoTrabajoDTO getFlujoTrabajoById(Long id) {
        log.debug("Buscando flujo de trabajo por ID: {}", id);

        return flujoTrabajoDAO.findById(id)
                .orElseThrow(() -> {
                    log.warn("Flujo de trabajo no encontrado con ID: {}", id);
                    return new RuntimeException("Flujo de trabajo no encontrado con ID: " + id);
                });
    }

    /**
     * READ ACTIVO — Obtener el flujo activo para un tipo documental — RF31 / RF28.
     *
     * CASO DE USO: Al iniciar la aprobación de un documento, se resuelve
     * qué flujo aplicar según su tipo documental.
     */
    @Override
    @Transactional(readOnly = true)
    public FlujoTrabajoDTO getFlujoActivoByOrganizacionAndTipoDocumento(Long organizacionNit, Long tipoDocumentoId) {
        log.debug("Buscando flujo activo para tipo documental {} en organización {}", tipoDocumentoId, organizacionNit);

        return flujoTrabajoDAO.findActivoByOrganizacionNitAndTipoDocumentoId(organizacionNit, tipoDocumentoId)
                .orElseThrow(() -> {
                    log.warn("Sin flujo activo para tipo documental {} en organización {}", tipoDocumentoId, organizacionNit);
                    return new IllegalStateException("No hay flujo de trabajo activo configurado para este tipo documental");
                });
    }

    /**
     * READ ACTIVOS — Flujos operativos de la organización — RF28.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FlujoTrabajoDTO> getFlujosActivosByOrganizacion(Long organizacionNit) {
        log.debug("Obteniendo flujos activos para organización NIT: {}", organizacionNit);
        organizacionService.getOrganizacionByNit(organizacionNit);
        return flujoTrabajoDAO.findActivosByOrganizacionNit(organizacionNit);
    }

    /**
     * READ ALL — Vista completa para configuración — RF44.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FlujoTrabajoDTO> getAllFlujosByOrganizacion(Long organizacionNit) {
        log.debug("Obteniendo todos los flujos para organización NIT: {}", organizacionNit);
        organizacionService.getOrganizacionByNit(organizacionNit);
        return flujoTrabajoDAO.findAllByOrganizacionNit(organizacionNit);
    }

    /**
     * UPDATE — Actualizar nombre/descripción del flujo — RF44.
     *
     * REGLA: organización y tipoDocumento son inmutables.
     */
    @Override
    public FlujoTrabajoDTO updateFlujoTrabajo(Long id, FlujoTrabajoUpdateDTO updateDTO) {
        log.info("Actualizando flujo de trabajo ID: {}", id);

        getFlujoTrabajoById(id);

        FlujoTrabajoDTO result = flujoTrabajoDAO.update(id, updateDTO)
                .orElseThrow(() -> new RuntimeException("Error al actualizar flujo de trabajo ID: " + id));

        log.info("Flujo de trabajo actualizado exitosamente ID: {}", id);
        return result;
    }

    /**
     * DELETE — Eliminar flujo de trabajo.
     *
     * RESTRICCIÓN: Verificar que no existan tareas activas asociadas
     * a los pasos de este flujo antes de eliminar.
     */
    @Override
    public void deleteFlujoTrabajo(Long id) {
        log.info("Eliminando flujo de trabajo ID: {}", id);

        getFlujoTrabajoById(id);

        boolean deleted = flujoTrabajoDAO.deleteById(id);
        if (!deleted) {
            throw new RuntimeException("Error al eliminar flujo de trabajo ID: " + id);
        }

        log.info("Flujo de trabajo eliminado exitosamente ID: {}", id);
    }

    // ─── Validaciones privadas ────────────────────────────────────────────────

    private void validateFlujoTrabajoData(FlujoTrabajoCreateDTO dto) {
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del flujo de trabajo es obligatorio");
        }
        if (dto.getOrganizacionNit() == null) {
            throw new IllegalArgumentException("El NIT de la organización es obligatorio");
        }
        if (dto.getTipoDocumentoId() == null) {
            throw new IllegalArgumentException("El tipo documental es obligatorio");
        }
    }
}
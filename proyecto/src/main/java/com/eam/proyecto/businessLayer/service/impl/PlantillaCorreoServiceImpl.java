// PlantillaCorreoServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.PlantillaCorreoCreateDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoUpdateDTO;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.businessLayer.service.PlantillaCorreoService;
import com.eam.proyecto.persistenceLayer.dao.PlantillaCorreoDAO;
import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PlantillaCorreoServiceImpl implements PlantillaCorreoService {

    private final PlantillaCorreoDAO plantillaCorreoDAO;
    private final OrganizacionService organizacionService;

    /**
     * CREATE — Crear plantilla de correo para un evento — RF40 / RF43.
     *
     * FLUJO:
     * 1. Validar datos obligatorios
     * 2. Verificar que la organización existe
     * 3. REGLA CRÍTICA: solo una plantilla activa por tipoEvento por organización
     * 4. Asignar activo=true
     * 5. Persistir
     */
    @Override
    public PlantillaCorreoDTO createPlantillaCorreo(PlantillaCorreoCreateDTO createDTO) {
        log.info("Creando plantilla de correo para evento {} en organización NIT: {}",
                createDTO.getTipoEvento(), createDTO.getOrganizacionNit());

        validatePlantillaData(createDTO);
        organizacionService.getOrganizacionActivaByNit(createDTO.getOrganizacionNit());

        // REGLA CRÍTICA: solo una plantilla activa por evento por organización — RF40 / RF43
        if (plantillaCorreoDAO.existeActivaByOrganizacionNitAndTipoEvento(
                createDTO.getOrganizacionNit(), createDTO.getTipoEvento())) {
            log.warn("Ya existe plantilla activa para evento {} en organización NIT: {}",
                    createDTO.getTipoEvento(), createDTO.getOrganizacionNit());
            throw new IllegalStateException(
                    "Ya existe una plantilla activa para el evento " + createDTO.getTipoEvento()
                            + ". Desactívela antes de crear una nueva");
        }

        // El service asigna activo=true al crear
        createDTO.setActivo(true);

        PlantillaCorreoDTO result = plantillaCorreoDAO.save(createDTO);

        log.info("Plantilla de correo creada exitosamente con ID: {}", result.getId());
        return result;
    }

    /**
     * READ — Buscar plantilla por ID.
     */
    @Override
    @Transactional(readOnly = true)
    public PlantillaCorreoDTO getPlantillaCorreoById(Long id) {
        log.debug("Buscando plantilla de correo por ID: {}", id);

        return plantillaCorreoDAO.findById(id)
                .orElseThrow(() -> {
                    log.warn("Plantilla de correo no encontrada con ID: {}", id);
                    return new RuntimeException("Plantilla de correo no encontrada con ID: " + id);
                });
    }

    /**
     * READ ACTIVA POR EVENTO — Obtener la plantilla activa para disparar una notificación.
     *
     * RF37: Correo al crear documento → TipoEventoEnum.DOCUMENTO_CREADO
     * RF38: Notificar cambio de estado → TipoEventoEnum.DOCUMENTO_APROBADO / RECHAZADO
     * RF39: Alertas de tareas → TipoEventoEnum.TAREA_ASIGNADA / TAREA_VENCIDA
     *
     * RETORNA excepción si no hay plantilla configurada, para que el service
     * llamante decida si el error es crítico o se omite la notificación.
     */
    @Override
    @Transactional(readOnly = true)
    public PlantillaCorreoDTO getPlantillaActivaByOrganizacionAndEvento(Long organizacionNit, TipoEventoEnum tipoEvento) {
        log.debug("Buscando plantilla activa para evento {} en organización NIT: {}", tipoEvento, organizacionNit);

        return plantillaCorreoDAO.findActivaByOrganizacionNitAndTipoEvento(organizacionNit, tipoEvento)
                .orElseThrow(() -> {
                    log.warn("Sin plantilla activa para evento {} en organización NIT: {}", tipoEvento, organizacionNit);
                    return new IllegalStateException(
                            "No hay plantilla de correo activa configurada para el evento: " + tipoEvento);
                });
    }

    /**
     * READ ACTIVAS — Plantillas disponibles para notificaciones — RF40.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PlantillaCorreoDTO> getPlantillasActivasByOrganizacion(Long organizacionNit) {
        log.debug("Obteniendo plantillas activas para organización NIT: {}", organizacionNit);
        organizacionService.getOrganizacionByNit(organizacionNit);
        return plantillaCorreoDAO.findActivasByOrganizacionNit(organizacionNit);
    }

    /**
     * READ ALL — Vista completa de configuración (activas e inactivas) — RF43.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PlantillaCorreoDTO> getAllPlantillasByOrganizacion(Long organizacionNit) {
        log.debug("Obteniendo todas las plantillas para organización NIT: {}", organizacionNit);
        organizacionService.getOrganizacionByNit(organizacionNit);
        return plantillaCorreoDAO.findAllByOrganizacionNit(organizacionNit);
    }

    /**
     * UPDATE — Actualizar asunto y cuerpo de la plantilla — RF40 / RF43.
     *
     * RESTRICCIONES: organizacion, tipoEvento y activo son inmutables aquí.
     * Para cambiar activo usar activar() / desactivar().
     */
    @Override
    public PlantillaCorreoDTO updatePlantillaCorreo(Long id, PlantillaCorreoUpdateDTO updateDTO) {
        log.info("Actualizando plantilla de correo ID: {}", id);

        getPlantillaCorreoById(id);
        validatePlantillaUpdateData(updateDTO);

        PlantillaCorreoDTO result = plantillaCorreoDAO.update(id, updateDTO)
                .orElseThrow(() -> new RuntimeException("Error al actualizar plantilla de correo ID: " + id));

        log.info("Plantilla de correo actualizada exitosamente ID: {}", id);
        return result;
    }

    /**
     * ACTIVAR — Reactivar plantilla previamente desactivada — RF40 / RF43.
     *
     * PRECONDICIÓN: No debe existir ya otra plantilla activa para el mismo
     * tipoEvento en la misma organización.
     */
    @Override
    public PlantillaCorreoDTO activarPlantilla(Long id) {
        log.info("Activando plantilla de correo ID: {}", id);

        PlantillaCorreoDTO plantilla = getPlantillaCorreoById(id);

        if (Boolean.TRUE.equals(plantilla.getActivo())) {
            throw new IllegalStateException("La plantilla ya se encuentra activa");
        }

        // Verificar que no haya otra activa para el mismo evento
        if (plantillaCorreoDAO.existeActivaByOrganizacionNitAndTipoEvento(
                plantilla.getOrganizacionNit(), plantilla.getTipoEvento())) {
            throw new IllegalStateException(
                    "Ya existe una plantilla activa para el evento " + plantilla.getTipoEvento()
                            + ". Desactívela antes de activar esta");
        }

        PlantillaCorreoDTO result = plantillaCorreoDAO.activar(id)
                .orElseThrow(() -> new RuntimeException("Error al activar plantilla de correo ID: " + id));

        log.info("Plantilla de correo activada exitosamente ID: {}", id);
        return result;
    }

    /**
     * DESACTIVAR — Desactivar plantilla sin eliminarla — RF40.
     *
     * POLÍTICA: No se elimina físicamente porque las notificaciones ya enviadas
     * mantienen referencia a esta plantilla. Solo se desactiva.
     */
    @Override
    public PlantillaCorreoDTO desactivarPlantilla(Long id) {
        log.info("Desactivando plantilla de correo ID: {}", id);

        PlantillaCorreoDTO plantilla = getPlantillaCorreoById(id);

        if (Boolean.FALSE.equals(plantilla.getActivo())) {
            throw new IllegalStateException("La plantilla ya se encuentra inactiva");
        }

        PlantillaCorreoDTO result = plantillaCorreoDAO.desactivar(id)
                .orElseThrow(() -> new RuntimeException("Error al desactivar plantilla de correo ID: " + id));

        log.info("Plantilla de correo desactivada exitosamente ID: {}", id);
        return result;
    }

    /**
     * DELETE — Eliminar plantilla físicamente.
     *
     * RESTRICCIÓN: Solo si no tiene notificaciones asociadas.
     * En la mayoría de casos se prefiere desactivar().
     */
    @Override
    public void deletePlantillaCorreo(Long id) {
        log.info("Eliminando plantilla de correo ID: {}", id);

        getPlantillaCorreoById(id);

        boolean deleted = plantillaCorreoDAO.deleteById(id);
        if (!deleted) {
            throw new RuntimeException("Error al eliminar plantilla de correo ID: " + id);
        }

        log.info("Plantilla de correo eliminada exitosamente ID: {}", id);
    }

    // ─── Validaciones privadas ────────────────────────────────────────────────

    private void validatePlantillaData(PlantillaCorreoCreateDTO dto) {
        if (dto.getTipoEvento() == null) {
            throw new IllegalArgumentException("El tipo de evento es obligatorio");
        }
        if (dto.getAsunto() == null || dto.getAsunto().trim().isEmpty()) {
            throw new IllegalArgumentException("El asunto de la plantilla es obligatorio");
        }
        if (dto.getCuerpo() == null || dto.getCuerpo().trim().isEmpty()) {
            throw new IllegalArgumentException("El cuerpo de la plantilla es obligatorio");
        }
        if (dto.getOrganizacionNit() == null) {
            throw new IllegalArgumentException("El NIT de la organización es obligatorio");
        }
    }

    private void validatePlantillaUpdateData(PlantillaCorreoUpdateDTO dto) {
        if (dto.getAsunto() != null && dto.getAsunto().trim().isEmpty()) {
            throw new IllegalArgumentException("El asunto no puede estar vacío");
        }
        if (dto.getCuerpo() != null && dto.getCuerpo().trim().isEmpty()) {
            throw new IllegalArgumentException("El cuerpo de la plantilla no puede estar vacío");
        }
    }
}
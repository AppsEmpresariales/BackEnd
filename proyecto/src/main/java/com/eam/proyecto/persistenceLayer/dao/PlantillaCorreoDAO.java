package com.eam.proyecto.persistenceLayer.dao;

import com.eam.proyecto.businessLayer.dto.PlantillaCorreoCreateDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoUpdateDTO;
import com.eam.proyecto.persistenceLayer.mapper.PlantillaCorreoMapper;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.PlantillaCorreoEntity;
import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import com.eam.proyecto.persistenceLayer.repository.PlantillaCorreoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de plantillas de correo.
 *
 * DESCRIPCION:
 * - PlantillaCorreoEntity define las plantillas reutilizables para notificaciones.
 * - Cada plantilla está ligada a un TipoEventoEnum (DOCUMENTO_CREADO,
 *   TAREA_ASIGNADA, etc.) y a una organización.
 * - Solo puede haber una plantilla activa por tipo de evento por organización.
 * - activo se gestiona mediante activar() y desactivar(), no en update().
 * - tipoEvento es inmutable: la plantilla no cambia el evento que cubre.
 *
 * HISTORIAS CUBIERTAS:
 * - US-037 (RF37): Correo al crear documento → findActivaByOrganizacionNitAndTipoEvento
 * - US-038 (RF38): Notificar cambios de estado → findActivaByOrganizacionNitAndTipoEvento
 * - US-039 (RF39): Alertas de tareas → findActivaByOrganizacionNitAndTipoEvento
 * - US-040 (RF40): Gestionar plantillas reutilizables → CRUD completo
 * - US-043 (RF43): Configurar plantillas por evento del sistema → update, activar/desactivar
 */
@Repository
@RequiredArgsConstructor
public class PlantillaCorreoDAO {

    private final PlantillaCorreoRepository plantillaCorreoRepository;
    private final PlantillaCorreoMapper plantillaCorreoMapper;

    /**
     * Crear una nueva plantilla de correo para una organización.
     *
     * FLUJO:
     * 1. CreateDTO → Entity (mapper ignora id y activo)
     * 2. El service asigna activo=true al crear (US-040)
     * 3. Guardar Entity → DTO
     *
     * US-040 / US-043
     */
    public PlantillaCorreoDTO save(PlantillaCorreoCreateDTO createDTO) {
        PlantillaCorreoEntity entity = plantillaCorreoMapper.toEntity(createDTO);
        return plantillaCorreoMapper.toDTO(plantillaCorreoRepository.save(entity));
    }

    /**
     * Buscar plantilla por ID (sin restricción de organización).
     *
     * ADVERTENCIA: Usar findByIdAndOrganizacionNit para operaciones multi-tenant.
     */
    public Optional<PlantillaCorreoDTO> findById(Long id) {
        return plantillaCorreoRepository.findById(id)
                .map(plantillaCorreoMapper::toDTO);
    }

    /**
     * Buscar plantilla por ID restringida a una organización.
     *
     * US-010: Aislamiento lógico — una plantilla solo es accesible por su organización.
     */
    public Optional<PlantillaCorreoDTO> findByIdAndOrganizacionNit(Long id, Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return plantillaCorreoRepository.findByIdAndOrganizacion(id, org)
                .map(plantillaCorreoMapper::toDTO);
    }

    /**
     * Actualizar contenido de una plantilla (asunto y cuerpo).
     *
     * RESTRICCIONES (aplicadas por el mapper):
     * - id, organizacion, activo, tipoEvento son inmutables.
     *
     * US-040 / US-043
     */
    public Optional<PlantillaCorreoDTO> update(Long id, PlantillaCorreoUpdateDTO updateDTO) {
        return plantillaCorreoRepository.findById(id)
                .map(existing -> {
                    plantillaCorreoMapper.updateEntityFromDTO(updateDTO, existing);
                    return plantillaCorreoMapper.toDTO(plantillaCorreoRepository.save(existing));
                });
    }

    /**
     * Desactivar una plantilla.
     *
     * POLÍTICA: No se eliminan físicamente porque las notificaciones enviadas
     * ya hacen referencia a la plantilla. Se desactiva para que no sea usada.
     * US-040
     */
    public Optional<PlantillaCorreoDTO> desactivar(Long id) {
        return plantillaCorreoRepository.findById(id)
                .map(existing -> {
                    existing.setActivo(false);
                    return plantillaCorreoMapper.toDTO(plantillaCorreoRepository.save(existing));
                });
    }

    /**
     * Activar una plantilla previamente desactivada.
     *
     * PRECONDICIÓN: El service verifica que no haya ya otra plantilla activa
     * para el mismo tipoEvento en la misma organización.
     * US-040 / US-043
     */
    public Optional<PlantillaCorreoDTO> activar(Long id) {
        return plantillaCorreoRepository.findById(id)
                .map(existing -> {
                    existing.setActivo(true);
                    return plantillaCorreoMapper.toDTO(plantillaCorreoRepository.save(existing));
                });
    }

    /**
     * Eliminar plantilla físicamente.
     *
     * RESTRICCIÓN: Solo si no tiene notificaciones asociadas. El service debe verificar.
     */
    public boolean deleteById(Long id) {
        if (plantillaCorreoRepository.existsById(id)) {
            plantillaCorreoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Listar plantillas activas de una organización.
     *
     * US-040: Vista de plantillas disponibles para usar en notificaciones.
     */
    public List<PlantillaCorreoDTO> findActivasByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return plantillaCorreoMapper.toDTOList(
                plantillaCorreoRepository.findByOrganizacionAndActivoTrue(org));
    }

    /**
     * Listar todas las plantillas de una organización (activas e inactivas).
     *
     * US-043: Vista completa de configuración de plantillas.
     */
    public List<PlantillaCorreoDTO> findAllByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return plantillaCorreoMapper.toDTOList(
                plantillaCorreoRepository.findByOrganizacion(org));
    }

    /**
     * Obtener la plantilla activa para un evento específico de la organización.
     *
     * US-037 / US-038 / US-039: Al disparar una notificación, el service
     * busca la plantilla activa para el tipo de evento correspondiente.
     * RETORNA: Optional vacío si el evento no tiene plantilla configurada.
     */
    public Optional<PlantillaCorreoDTO> findActivaByOrganizacionNitAndTipoEvento(
            Long nit, TipoEventoEnum tipoEvento) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return plantillaCorreoRepository
                .findByOrganizacionAndTipoEventoAndActivoTrue(org, tipoEvento)
                .map(plantillaCorreoMapper::toDTO);
    }

    /**
     * Listar plantillas de una organización por tipo de evento (todas las versiones).
     *
     * US-043: Ver el historial de plantillas para un evento (activas e inactivas).
     */
    public List<PlantillaCorreoDTO> findByOrganizacionNitAndTipoEvento(
            Long nit, TipoEventoEnum tipoEvento) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return plantillaCorreoMapper.toDTOList(
                plantillaCorreoRepository.findByOrganizacionAndTipoEvento(org, tipoEvento));
    }

    /**
     * Verificar si ya existe una plantilla activa para un tipo de evento.
     *
     * US-040 / US-043: Evitar duplicados al crear o activar una nueva plantilla.
     */
    public boolean existeActivaByOrganizacionNitAndTipoEvento(Long nit, TipoEventoEnum tipoEvento) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return plantillaCorreoRepository
                .existsByOrganizacionAndTipoEventoAndActivoTrue(org, tipoEvento);
    }

    /**
     * Contar total de plantillas.
     */
    public long count() {
        return plantillaCorreoRepository.count();
    }

    // ─── Método auxiliar privado ──────────────────────────────────────────────

    private OrganizacionEntity buildOrganizacionRef(Long nit) {
        OrganizacionEntity org = new OrganizacionEntity();
        org.setNit(nit);
        return org;
    }
}

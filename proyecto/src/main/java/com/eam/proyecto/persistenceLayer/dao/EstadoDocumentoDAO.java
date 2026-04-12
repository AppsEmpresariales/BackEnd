package com.eam.proyecto.persistenceLayer.dao;

import com.eam.proyecto.businessLayer.dto.EstadoDocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.EstadoDocumentoDTO;
import com.eam.proyecto.businessLayer.dto.EstadoDocumentoUpdateDTO;
import com.eam.proyecto.persistenceLayer.mapper.EstadoDocumentoMapper;
import com.eam.proyecto.persistenceLayer.entity.EstadoDocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.repository.EstadoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de estados de documentos.
 *
 * DESCRIPCION:
 * - EstadoDocumentoEntity es un catálogo real en BD (NOT un enum de Java).
 * - Cada organización puede tener sus propios estados personalizados.
 * - esInicial=true: estado asignado al crear un documento (solo uno por organización).
 * - esFinal=true: el documento no puede avanzar más en el flujo.
 *
 * HISTORIAS CUBIERTAS:
 * - US-030 (RF30): Cambiar estado del documento → findByIdAndOrganizacionNit
 * - US-031 (RF31): Validar secuencia del flujo → findInicial, findFinales
 * - US-041 (RF41): Parametrizar estados de documentos → CRUD completo
 */
@Repository
@RequiredArgsConstructor
public class EstadoDocumentoDAO {

    private final EstadoDocumentoRepository estadoDocumentoRepository;
    private final EstadoDocumentoMapper estadoDocumentoMapper;

    /**
     * Crear un nuevo estado documental para una organización.
     *
     * FLUJO:
     * 1. CreateDTO → Entity (mapper establece FK organizacion con NIT)
     * 2. El service valida que no haya conflicto de esInicial antes de llamar aquí
     * 3. Guardar Entity → DTO
     *
     * US-041
     */
    public EstadoDocumentoDTO save(EstadoDocumentoCreateDTO createDTO) {
        EstadoDocumentoEntity entity = estadoDocumentoMapper.toEntity(createDTO);
        return estadoDocumentoMapper.toDTO(estadoDocumentoRepository.save(entity));
    }

    /**
     * Buscar estado por ID (sin restricción de organización).
     *
     * ADVERTENCIA: Usar findByIdAndOrganizacionNit en operaciones multi-tenant.
     */
    public Optional<EstadoDocumentoDTO> findById(Long id) {
        return estadoDocumentoRepository.findById(id)
                .map(estadoDocumentoMapper::toDTO);
    }

    /**
     * Buscar estado por ID restringido a una organización.
     *
     * US-010: Aislamiento lógico de datos entre organizaciones.
     */
    public Optional<EstadoDocumentoDTO> findByIdAndOrganizacionNit(Long id, Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return estadoDocumentoRepository.findByIdAndOrganizacion(id, org)
                .map(estadoDocumentoMapper::toDTO);
    }

    /**
     * Actualizar datos de un estado existente.
     *
     * RESTRICCIONES (aplicadas por el mapper):
     * - id, organizacion son inmutables.
     *
     * US-041
     */
    public Optional<EstadoDocumentoDTO> update(Long id, EstadoDocumentoUpdateDTO updateDTO) {
        return estadoDocumentoRepository.findById(id)
                .map(existing -> {
                    estadoDocumentoMapper.updateEntityFromDTO(updateDTO, existing);
                    return estadoDocumentoMapper.toDTO(estadoDocumentoRepository.save(existing));
                });
    }

    /**
     * Eliminar estado por ID.
     *
     * RESTRICCIÓN: El service debe verificar que ningún documento o flujo
     * lo esté usando antes de eliminar físicamente.
     */
    public boolean deleteById(Long id) {
        if (estadoDocumentoRepository.existsById(id)) {
            estadoDocumentoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Listar todos los estados de una organización.
     *
     * US-041: Vista completa del catálogo de estados para configuración.
     */
    public List<EstadoDocumentoDTO> findByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return estadoDocumentoMapper.toDTOList(
                estadoDocumentoRepository.findByOrganizacion(org));
    }

    /**
     * Obtener el estado inicial de la organización.
     *
     * US-017: Al crear un documento, se le asigna el estado inicial.
     * US-031: Validar que el flujo comienza desde el estado correcto.
     * RETORNA: Optional vacío si la organización no tiene estado inicial configurado.
     */
    public Optional<EstadoDocumentoDTO> findInicialByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return estadoDocumentoRepository.findByOrganizacionAndEsInicialTrue(org)
                .map(estadoDocumentoMapper::toDTO);
    }

    /**
     * Obtener los estados finales de la organización.
     *
     * US-031: Validar si el documento llegó al final del flujo.
     * US-030: Verificar si el cambio de estado cierra el proceso.
     */
    public List<EstadoDocumentoDTO> findFinalesByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return estadoDocumentoMapper.toDTOList(
                estadoDocumentoRepository.findByOrganizacionAndEsFinalTrue(org));
    }

    /**
     * Verificar si un estado es final por ID.
     *
     * US-030: Antes de completar una tarea, verificar si el objetivo
     * es un estado final para cerrar el flujo.
     */
    public boolean esFinal(Long id) {
        return estadoDocumentoRepository.findByIdAndEsFinalTrue(id).isPresent();
    }

    /**
     * Listar estados no finales de una organización.
     *
     * CASO DE USO: Mostrar estados disponibles para avanzar en el flujo.
     */
    public List<EstadoDocumentoDTO> findNoFinalesByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return estadoDocumentoMapper.toDTOList(
                estadoDocumentoRepository.findByOrganizacionAndEsFinalFalse(org));
    }

    /**
     * Buscar estado por nombre dentro de una organización.
     *
     * CASO DE USO: Validar unicidad de nombre al crear o editar estado.
     */
    public Optional<EstadoDocumentoDTO> findByNombreAndOrganizacionNit(String nombre, Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return estadoDocumentoRepository.findByNombreAndOrganizacion(nombre, org)
                .map(estadoDocumentoMapper::toDTO);
    }

    /**
     * Contar total de estados.
     */
    public long count() {
        return estadoDocumentoRepository.count();
    }

    // ─── Método auxiliar privado ──────────────────────────────────────────────

    private OrganizacionEntity buildOrganizacionRef(Long nit) {
        OrganizacionEntity org = new OrganizacionEntity();
        org.setNit(nit);
        return org;
    }
}

package com.eam.proyecto.persistenceLayer.dao;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoUpdateDTO;
import com.eam.proyecto.persistenceLayer.mapper.FlujoTrabajoMapper;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoEntity;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.TipoDocumentoEntity;
import com.eam.proyecto.persistenceLayer.repository.FlujoTrabajoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de flujos de trabajo.
 *
 * DESCRIPCION:
 * - FlujoTrabajoEntity define el proceso de aprobación por tipo documental.
 * - Solo puede existir un flujo activo por tipo documental por organización.
 * - tipoDocumento es inmutable: no se puede cambiar el tipo de un flujo existente.
 * - Los pasos del flujo se gestionan en FlujoTrabajoPasoDAO.
 *
 * HISTORIAS CUBIERTAS:
 * - US-028 (RF28): Definir flujo de aprobación → save(createDTO)
 * - US-031 (RF31): Validar secuencia → findActivoByOrganizacionNitAndTipoDocumentoId
 * - US-032 (RF32): Parametrizar flujos por tipo documental → findByOrganizacionNitAndTipoDocumentoId
 * - US-044 (RF44): Configurar flujos de proceso → update(id, updateDTO)
 */
@Repository
@RequiredArgsConstructor
public class FlujoTrabajoDAO {

    private final FlujoTrabajoRepository flujoTrabajoRepository;
    private final FlujoTrabajoMapper flujoTrabajoMapper;

    /**
     * Crear un nuevo flujo de trabajo para una organización.
     *
     * FLUJO:
     * 1. CreateDTO → Entity (mapper convierte organizacionNit y tipoDocumentoId a entidades ref)
     * 2. El service verifica que no exista ya un flujo activo para ese tipo documental
     * 3. Guardar Entity → DTO
     *
     * US-028 / US-032
     */
    public FlujoTrabajoDTO save(FlujoTrabajoCreateDTO createDTO) {
        FlujoTrabajoEntity entity = flujoTrabajoMapper.toEntity(createDTO);
        return flujoTrabajoMapper.toDTO(flujoTrabajoRepository.save(entity));
    }

    /**
     * Buscar flujo por ID (sin restricción de organización).
     *
     * ADVERTENCIA: Usar findByIdAndOrganizacionNit para operaciones multi-tenant.
     */
    public Optional<FlujoTrabajoDTO> findById(Long id) {
        return flujoTrabajoRepository.findById(id)
                .map(flujoTrabajoMapper::toDTO);
    }

    /**
     * Buscar flujo por ID restringido a una organización.
     *
     * US-010: Aislamiento lógico entre organizaciones.
     */
    public Optional<FlujoTrabajoDTO> findByIdAndOrganizacionNit(Long id, Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return flujoTrabajoRepository.findByIdAndOrganizacion(id, org)
                .map(flujoTrabajoMapper::toDTO);
    }

    /**
     * Actualizar datos de un flujo existente.
     *
     * RESTRICCIONES (aplicadas por el mapper):
     * - id, organizacion, tipoDocumento son inmutables.
     *
     * US-044
     */
    public Optional<FlujoTrabajoDTO> update(Long id, FlujoTrabajoUpdateDTO updateDTO) {
        return flujoTrabajoRepository.findById(id)
                .map(existing -> {
                    flujoTrabajoMapper.updateEntityFromDTO(updateDTO, existing);
                    return flujoTrabajoMapper.toDTO(flujoTrabajoRepository.save(existing));
                });
    }

    /**
     * Eliminar flujo por ID.
     *
     * RESTRICCIÓN: El service debe verificar que no existan tareas activas
     * asociadas a los pasos de este flujo antes de eliminar.
     */
    public boolean deleteById(Long id) {
        if (flujoTrabajoRepository.existsById(id)) {
            flujoTrabajoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Listar flujos activos de una organización.
     *
     * US-028: Vista de flujos operativos para el administrador.
     */
    public List<FlujoTrabajoDTO> findActivosByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return flujoTrabajoMapper.toDTOList(
                flujoTrabajoRepository.findByOrganizacionAndActivoTrue(org));
    }

    /**
     * Listar todos los flujos de una organización (activos e inactivos).
     *
     * US-044: Vista completa para configuración.
     */
    public List<FlujoTrabajoDTO> findAllByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return flujoTrabajoMapper.toDTOList(flujoTrabajoRepository.findByOrganizacion(org));
    }

    /**
     * Obtener el flujo activo para un tipo documental dentro de una organización.
     *
     * US-031 / US-028: Al iniciar el proceso de aprobación de un documento,
     * se resuelve qué flujo aplicar según su tipo documental.
     * RETORNA: Optional vacío si el tipo documental no tiene flujo configurado.
     */
    public Optional<FlujoTrabajoDTO> findActivoByOrganizacionNitAndTipoDocumentoId(
            Long nit, Long tipoDocumentoId) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        TipoDocumentoEntity tipo = buildTipoDocumentoRef(tipoDocumentoId);
        return flujoTrabajoRepository
                .findByOrganizacionAndTipoDocumentoAndActivoTrue(org, tipo)
                .map(flujoTrabajoMapper::toDTO);
    }

    /**
     * Listar todos los flujos (activos e inactivos) para un tipo documental.
     *
     * US-032: Historial de flujos configurados para un tipo documental.
     */
    public List<FlujoTrabajoDTO> findByOrganizacionNitAndTipoDocumentoId(
            Long nit, Long tipoDocumentoId) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        TipoDocumentoEntity tipo = buildTipoDocumentoRef(tipoDocumentoId);
        return flujoTrabajoMapper.toDTOList(
                flujoTrabajoRepository.findByOrganizacionAndTipoDocumento(org, tipo));
    }

    /**
     * Verificar si ya existe un flujo activo para un tipo documental.
     *
     * US-032: Evitar múltiples flujos activos para el mismo tipo documental.
     */
    public boolean existeActivoPorTipoDocumental(Long nit, Long tipoDocumentoId) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        TipoDocumentoEntity tipo = buildTipoDocumentoRef(tipoDocumentoId);
        return flujoTrabajoRepository
                .existsByOrganizacionAndTipoDocumentoAndActivoTrue(org, tipo);
    }

    /**
     * Contar total de flujos de trabajo.
     */
    public long count() {
        return flujoTrabajoRepository.count();
    }

    // ─── Métodos auxiliares privados ─────────────────────────────────────────

    private OrganizacionEntity buildOrganizacionRef(Long nit) {
        OrganizacionEntity org = new OrganizacionEntity();
        org.setNit(nit);
        return org;
    }

    private TipoDocumentoEntity buildTipoDocumentoRef(Long id) {
        TipoDocumentoEntity tipo = new TipoDocumentoEntity();
        tipo.setId(id);
        return tipo;
    }
}

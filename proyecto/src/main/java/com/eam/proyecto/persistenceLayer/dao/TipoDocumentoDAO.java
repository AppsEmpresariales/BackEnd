package com.docucloud.persistence.dao;

import com.docucloud.businessLayer.dto.TipoDocumentoCreateDTO;
import com.docucloud.businessLayer.dto.TipoDocumentoDTO;
import com.docucloud.businessLayer.dto.TipoDocumentoUpdateDTO;
import com.docucloud.businessLayer.mapper.TipoDocumentoMapper;
import com.docucloud.persistence.entity.OrganizacionEntity;
import com.docucloud.persistence.entity.TipoDocumentoEntity;
import com.docucloud.persistence.repository.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de tipos documentales.
 *
 * DESCRIPCION:
 * - TipoDocumentoEntity es un catálogo por organización (multi-tenant).
 * - El campo 'active' se gestiona mediante desactivar() y activar(),
 *   no a través del update general.
 * - El nombre debe ser único dentro de la misma organización.
 *
 * HISTORIAS CUBIERTAS:
 * - US-024 (RF24): Crear tipo de documento personalizado → save(createDTO)
 * - US-025 (RF25): Editar tipo documental → update(id, updateDTO)
 * - US-026 (RF26): Eliminar (desactivar) tipo documental → desactivar(id)
 * - US-027 (RF27): Asociar documentos a un tipo documental → findById
 * - US-042 (RF42): Parametrizar tipos desde configuración → CRUD completo
 */
@Repository
@RequiredArgsConstructor
public class TipoDocumentoDAO {

    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final TipoDocumentoMapper tipoDocumentoMapper;

    /**
     * Crear un nuevo tipo documental para una organización.
     *
     * FLUJO:
     * 1. CreateDTO → Entity (mapper ignora id, creadoEn y active)
     * 2. El service asigna active=true antes de llamar aquí
     * 3. Guardar Entity → DTO con información de la organización
     *
     * US-024 / US-042
     */
    public TipoDocumentoDTO save(TipoDocumentoCreateDTO createDTO) {
        TipoDocumentoEntity entity = tipoDocumentoMapper.toEntity(createDTO);
        return tipoDocumentoMapper.toDTO(tipoDocumentoRepository.save(entity));
    }

    /**
     * Buscar tipo documental por ID (sin restricción de organización).
     *
     * ADVERTENCIA: Usar findByIdAndOrganizacionNit para operaciones multi-tenant.
     */
    public Optional<TipoDocumentoDTO> findById(Long id) {
        return tipoDocumentoRepository.findById(id)
                .map(tipoDocumentoMapper::toDTO);
    }

    /**
     * Buscar tipo documental por ID restringido a una organización.
     *
     * US-010: Aislamiento lógico — un tipo documental solo es visible
     * para su propia organización.
     */
    public Optional<TipoDocumentoDTO> findByIdAndOrganizacionNit(Long id, Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return tipoDocumentoRepository.findByIdAndOrganizacion(id, org)
                .map(tipoDocumentoMapper::toDTO);
    }

    /**
     * Actualizar datos de un tipo documental existente.
     *
     * RESTRICCIONES (aplicadas por el mapper):
     * - id, creadoEn, organizacion son inmutables.
     * - active: se cambia solo con desactivar() / activar().
     *
     * US-025 / US-042
     */
    public Optional<TipoDocumentoDTO> update(Long id, TipoDocumentoUpdateDTO updateDTO) {
        return tipoDocumentoRepository.findById(id)
                .map(existing -> {
                    tipoDocumentoMapper.updateEntityFromDTO(updateDTO, existing);
                    return tipoDocumentoMapper.toDTO(tipoDocumentoRepository.save(existing));
                });
    }

    /**
     * Desactivar (eliminación lógica) un tipo documental.
     *
     * POLÍTICA: No se eliminan físicamente porque los documentos existentes
     * siguen referenciando el tipo. Solo se marca como inactive.
     *
     * US-026
     */
    public Optional<TipoDocumentoDTO> desactivar(Long id) {
        return tipoDocumentoRepository.findById(id)
                .map(existing -> {
                    existing.setActive(false);
                    return tipoDocumentoMapper.toDTO(tipoDocumentoRepository.save(existing));
                });
    }

    /**
     * Activar un tipo documental previamente desactivado.
     *
     * CASO DE USO: Reactivación de tipos documentales (US-042).
     */
    public Optional<TipoDocumentoDTO> activar(Long id) {
        return tipoDocumentoRepository.findById(id)
                .map(existing -> {
                    existing.setActive(true);
                    return tipoDocumentoMapper.toDTO(tipoDocumentoRepository.save(existing));
                });
    }

    /**
     * Eliminar tipo documental físicamente.
     *
     * RESTRICCIÓN: Solo si no tiene documentos asociados. El service debe verificar.
     */
    public boolean deleteById(Long id) {
        if (tipoDocumentoRepository.existsById(id)) {
            tipoDocumentoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Listar tipos documentales activos de una organización.
     *
     * US-024 / US-027: Selector de tipo al crear un documento.
     */
    public List<TipoDocumentoDTO> findActivosByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return tipoDocumentoMapper.toDTOList(
                tipoDocumentoRepository.findByOrganizacionAndActiveTrue(org));
    }

    /**
     * Listar todos los tipos documentales de una organización (activos e inactivos).
     *
     * US-042: Vista de configuración completa del catálogo.
     */
    public List<TipoDocumentoDTO> findAllByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return tipoDocumentoMapper.toDTOList(tipoDocumentoRepository.findByOrganizacion(org));
    }

    /**
     * Buscar tipo documental por nombre dentro de una organización.
     *
     * CASO DE USO: Validar unicidad de nombre antes de crear o renombrar.
     */
    public Optional<TipoDocumentoDTO> findByNombreAndOrganizacionNit(String nombre, Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return tipoDocumentoRepository.findByNombreAndOrganizacion(nombre, org)
                .map(tipoDocumentoMapper::toDTO);
    }

    /**
     * Verificar si ya existe un tipo con ese nombre en la organización.
     *
     * US-024 Escenario: Nombre duplicado dentro del mismo tenant.
     */
    public boolean existsByNombreAndOrganizacionNit(String nombre, Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return tipoDocumentoRepository.existsByNombreAndOrganizacion(nombre, org);
    }

    /**
     * Buscar tipos documentales por nombre (contiene texto).
     *
     * US-042: Buscador en la pantalla de configuración.
     */
    public List<TipoDocumentoDTO> findByOrganizacionNitAndNombreContaining(Long nit, String nombre) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return tipoDocumentoMapper.toDTOList(
                tipoDocumentoRepository.findByOrganizacionAndNombreContainingIgnoreCase(org, nombre));
    }

    /**
     * Contar tipos documentales activos de una organización.
     *
     * CASO DE USO: Dashboard del administrador.
     */
    public long countActivosByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return tipoDocumentoRepository.countByOrganizacionAndActiveTrue(org);
    }

    /**
     * Contar total de tipos documentales.
     */
    public long count() {
        return tipoDocumentoRepository.count();
    }

    // ─── Método auxiliar privado ──────────────────────────────────────────────

    private OrganizacionEntity buildOrganizacionRef(Long nit) {
        OrganizacionEntity org = new OrganizacionEntity();
        org.setNit(nit);
        return org;
    }
}

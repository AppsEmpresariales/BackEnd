package com.docucloud.persistence.dao;

import com.docucloud.businessLayer.dto.OrganizacionCreateDTO;
import com.docucloud.businessLayer.dto.OrganizacionDTO;
import com.docucloud.businessLayer.dto.OrganizacionUpdateDTO;
import com.docucloud.businessLayer.mapper.OrganizacionMapper;
import com.docucloud.persistence.entity.OrganizacionEntity;
import com.docucloud.persistence.repository.OrganizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de organizaciones.
 *
 * DESCRIPCION:
 * - OrganizacionEntity usa 'nit' como PK natural (no autogenerado).
 * - Soporta multi-tenancy: cada organización es un tenant aislado.
 * - Incluye validaciones de unicidad (NIT, email).
 *
 * HISTORIAS CUBIERTAS:
 * - US-001 (RF01): Registrar organización en la plataforma → save(createDTO)
 * - US-008 (RF08): Crear organización (tenant) en el sistema → save(createDTO)
 * - US-010 (RF10): Garantizar aislamiento lógico entre organizaciones → findByNit
 * - US-011 (RF11): Gestionar datos de la organización como admin → update(nit, updateDTO)
 */
@Repository
@RequiredArgsConstructor
public class OrganizacionDAO {

    private final OrganizacionRepository organizacionRepository;
    private final OrganizacionMapper organizacionMapper;

    /**
     * Crear una nueva organización (registro inicial / tenant).
     *
     * FLUJO:
     * 1. CreateDTO → Entity (mapper ignora creadoEn y active)
     * 2. El service asigna active=true y creadoEn antes de pasar el DTO
     * 3. Guardar Entity → DTO
     *
     * US-001 / US-008
     */
    public OrganizacionDTO save(OrganizacionCreateDTO createDTO) {
        OrganizacionEntity entity = organizacionMapper.toEntity(createDTO);
        OrganizacionEntity saved = organizacionRepository.save(entity);
        return organizacionMapper.toDTO(saved);
    }

    /**
     * Buscar organización por NIT.
     *
     * US-010: Aislamiento de datos — todas las operaciones de negocio
     * comienzan resolviendo la OrganizacionEntity por NIT.
     */
    public Optional<OrganizacionDTO> findByNit(Long nit) {
        return organizacionRepository.findByNit(nit)
                .map(organizacionMapper::toDTO);
    }

    /**
     * Buscar todas las organizaciones.
     *
     * CASO DE USO: Panel de administración global del sistema.
     */
    public List<OrganizacionDTO> findAll() {
        return organizacionMapper.toDTOList(organizacionRepository.findAll());
    }

    /**
     * Actualizar datos de la organización.
     *
     * RESTRICCIÓN: NIT es inmutable (PK natural). El mapper lo ignora.
     * US-011
     */
    public Optional<OrganizacionDTO> update(Long nit, OrganizacionUpdateDTO updateDTO) {
        return organizacionRepository.findByNit(nit)
                .map(existing -> {
                    organizacionMapper.updateEntityFromDTO(updateDTO, existing);
                    return organizacionMapper.toDTO(organizacionRepository.save(existing));
                });
    }

    /**
     * Eliminar organización por NIT.
     *
     * NOTA: En producción se prefiere desactivar (active=false) antes que eliminar.
     */
    public boolean deleteByNit(Long nit) {
        if (organizacionRepository.existsByNit(nit)) {
            organizacionRepository.deleteById(nit);
            return true;
        }
        return false;
    }

    /**
     * Verificar si el NIT ya está registrado.
     *
     * US-001 Escenario 2: "NIT ya registrado" → devuelve true si existe.
     */
    public boolean existsByNit(Long nit) {
        return organizacionRepository.existsByNit(nit);
    }

    /**
     * Verificar si el email ya está registrado.
     *
     * CASO DE USO: Validación de unicidad de email en el registro.
     */
    public boolean existsByEmail(String email) {
        return organizacionRepository.existsByEmail(email);
    }

    /**
     * Buscar organización por email.
     *
     * CASO DE USO: Recuperación de cuenta, validaciones de negocio.
     */
    public Optional<OrganizacionDTO> findByEmail(String email) {
        return organizacionRepository.findByEmail(email)
                .map(organizacionMapper::toDTO);
    }

    /**
     * Listar organizaciones activas.
     *
     * CASO DE USO: Panel global — solo tenants operativos.
     */
    public List<OrganizacionDTO> findActivas() {
        return organizacionMapper.toDTOList(organizacionRepository.findByActiveTrue());
    }

    /**
     * Listar organizaciones inactivas.
     *
     * CASO DE USO: Auditoría, reactivaciones administrativas.
     */
    public List<OrganizacionDTO> findInactivas() {
        return organizacionMapper.toDTOList(organizacionRepository.findByActiveFalse());
    }

    /**
     * Buscar organizaciones por nombre (contiene texto, ignora mayúsculas).
     *
     * CASO DE USO: Buscador en panel de administración.
     */
    public List<OrganizacionDTO> findByNombreContaining(String nombre) {
        return organizacionMapper.toDTOList(
                organizacionRepository.findByNombreContainingIgnoreCase(nombre));
    }

    /**
     * Buscar organización activa por NIT.
     *
     * CASO DE USO: Validación previa a login — solo organizaciones operativas.
     */
    public Optional<OrganizacionDTO> findActivaByNit(Long nit) {
        return organizacionRepository.findByNitAndActiveTrue(nit)
                .map(organizacionMapper::toDTO);
    }

    /**
     * Contar organizaciones activas.
     *
     * CASO DE USO: Dashboard global, estadísticas de la plataforma.
     */
    public long countActivas() {
        return organizacionRepository.countByActiveTrue();
    }

    /**
     * Contar total de organizaciones.
     *
     * CASO DE USO: Métricas generales del sistema.
     */
    public long count() {
        return organizacionRepository.count();
    }
}

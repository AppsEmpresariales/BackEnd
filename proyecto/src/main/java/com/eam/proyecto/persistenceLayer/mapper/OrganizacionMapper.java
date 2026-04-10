package com.eam.proyecto.persistenceLayer.mapper;

import com.eam.proyecto.businessLayer.dto.OrganizacionCreateDTO;
import com.eam.proyecto.businessLayer.dto.OrganizacionDTO;
import com.eam.proyecto.businessLayer.dto.OrganizacionUpdateDTO;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre OrganizacionEntity y DTOs usando MapStruct.
 *
 * COMPLEJIDAD:
 * - OrganizacionEntity usa 'nit' como PK (no @GeneratedValue), se mapea directamente.
 * - US-001: Registro de organización (RF01) → toEntity(OrganizacionCreateDTO)
 * - US-008: Crear organización/tenant (RF08) → mismo método
 * - US-011: Editar datos de organización como admin (RF11) → updateEntityFromDTO(...)
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface OrganizacionMapper {

    /**
     * Convierte OrganizacionEntity a OrganizacionDTO (LECTURA).
     * Mapeo directo: todos los campos coinciden en nombre.
     */
    OrganizacionDTO toDTO(OrganizacionEntity entity);

    /**
     * Convierte lista de OrganizacionEntity a lista de OrganizacionDTO.
     */
    List<OrganizacionDTO> toDTOList(List<OrganizacionEntity> entities);

    /**
     * Convierte OrganizacionCreateDTO a OrganizacionEntity (CREAR).
     *
     * CAMPOS IGNORADOS:
     * - creadoEn: lo gestiona JPA / el service antes de persistir.
     * - active: se establece en true por defecto en el service (US-001 → estado ACTIVO).
     */
    @Mapping(target = "creadoEn", ignore = true)
    @Mapping(target = "active", ignore = true)
    OrganizacionEntity toEntity(OrganizacionCreateDTO createDTO);

    /**
     * Actualiza OrganizacionEntity existente con datos de OrganizacionUpdateDTO.
     *
     * ESTRATEGIA IGNORE para valores null → actualización parcial (US-011).
     * NIT nunca cambia → ignorado.
     * creadoEn es inmutable → ignorado.
     */
    @Mapping(target = "nit", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(OrganizacionUpdateDTO updateDTO, @MappingTarget OrganizacionEntity entity);
}
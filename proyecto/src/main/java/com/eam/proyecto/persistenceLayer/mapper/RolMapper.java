package com.docucloud.businessLayer.mapper;

import com.docucloud.businessLayer.dto.RolDTO;
import com.docucloud.persistence.entity.RolEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para conversiones entre RolEntity y DTOs usando MapStruct.
 *
 *
 *
 * NOTA DE DISEÑO:
 * - RolEntity es un catálogo simple (id, nombre, descripcion).
 * - Los roles del sistema son ADMIN_ORG y USER_ESTANDAR (US-005).
 * - No se expone un CreateDTO ni UpdateDTO ya que los roles
 *   son datos maestros gestionados por el administrador del sistema (no por la app).
 *
 * HISTORIAS CUBIERTAS:
 * - US-005: Gestionar roles del sistema ADMIN_ORG y USER_ESTANDAR (RF05) → toDTO / toDTOList.
 * - US-006: Restringir acceso a recursos según rol (RF06) → campo nombre usado en Spring Security.
 * - US-015: Asignar o cambiar rol de un usuario (RF15) → referencia usada en RolUsuarioMapper.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface RolMapper {

    /** Convierte RolEntity a RolDTO (LECTURA). Todos los campos coinciden. */
    RolDTO toDTO(RolEntity entity);

    /** Convierte lista de RolEntity a lista de RolDTO. */
    List<RolDTO> toDTOList(List<RolEntity> entities);
}

package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de creación para RolUsuarioEntity.
 * Usado para asignar un rol a un usuario existente.
 *
 * RESTRICCIÓN DE NEGOCIO: Si el usuario ya tiene el rol asignado,
 * el service lanza excepción antes de intentar persistir
 * (constraint UNIQUE(user_id, rol_id) en BD).
 *
 * US-015
 */
@Data
public class RolUsuarioAsignarDTO {

    @NotNull(message = "La cédula del usuario es obligatoria")
    private Long usuarioCedula;

    @NotNull(message = "El ID del rol es obligatorio")
    private Long rolId;
}

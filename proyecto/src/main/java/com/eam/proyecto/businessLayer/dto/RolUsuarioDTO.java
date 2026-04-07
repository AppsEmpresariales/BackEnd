package com.docucloud.businessLayer.dto;

import lombok.Data;

/**
 * DTO de lectura para RolUsuarioEntity.
 * Expone información denormalizada del usuario y del rol asignado.
 *
 * US-005 / US-006 / US-015
 */
@Data
public class RolUsuarioDTO {

    private Long id;

    /** Denormalizado desde usuario.cedula */
    private Long usuarioCedula;

    /** Denormalizado desde usuario.nombre */
    private String usuarioNombre;

    /** Denormalizado desde rol.id */
    private Long rolId;

    /** Denormalizado desde rol.nombre */
    private String rolNombre;
}

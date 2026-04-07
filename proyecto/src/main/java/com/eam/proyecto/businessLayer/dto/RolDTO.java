package com.docucloud.businessLayer.dto;

import lombok.Data;

/**
 * DTO de lectura para RolEntity.
 * Catálogo simple: ADMIN_ORG y USER_ESTANDAR.
 *
 * NOTA: No existe CreateDTO ni UpdateDTO porque los roles
 * son datos maestros gestionados por el administrador del sistema,
 * no por la aplicación.
 *
 * US-005 / US-006
 */
@Data
public class RolDTO {

    private Long id;
    private String nombre;
    private String descripcion;
}

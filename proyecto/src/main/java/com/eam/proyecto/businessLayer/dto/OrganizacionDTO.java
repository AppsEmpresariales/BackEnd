package com.docucloud.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO de lectura para OrganizacionEntity.
 * Expone todos los campos de la organización (tenant).
 *
 * US-008 / US-011 / US-010
 */
@Data
public class OrganizacionDTO {

    private Long nit;
    private String nombre;
    private String email;
    private String telefono;
    private String dirCalle;
    private String dirNumero;
    private String dirComuna;
    private Boolean active;
    private LocalDateTime creadoEn;
}

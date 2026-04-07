package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de actualización para OrganizacionEntity.
 * Todos los campos son opcionales (actualización parcial PATCH).
 *
 * CAMPOS NO ACTUALIZABLES (ignorados por el mapper):
 * - nit: PK natural inmutable.
 * - creadoEn: inmutable.
 *
 * US-011
 */
@Data
public class OrganizacionUpdateDTO {

    @Size(max = 200)
    private String nombre;

    @Email(message = "Formato de correo inválido")
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String telefono;

    @Size(max = 200)
    private String dirCalle;

    @Size(max = 20)
    private String dirNumero;

    @Size(max = 100)
    private String dirComuna;

    /** Activar / inactivar la organización (US-011). */
    private Boolean active;
}

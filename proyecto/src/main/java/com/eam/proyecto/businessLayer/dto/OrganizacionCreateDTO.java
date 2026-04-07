package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de creación para OrganizacionEntity.
 * Usado en el registro inicial de una organización / tenant.
 *
 * CAMPOS OMITIDOS (los gestiona el service):
 * - active: se establece en true al crear (US-001 → estado ACTIVO).
 * - creadoEn: asignado por el service antes de persistir.
 *
 * US-001 / US-008
 */
@Data
public class OrganizacionCreateDTO {

    @NotNull(message = "El NIT es obligatorio")
    private Long nit;

    @NotBlank(message = "El nombre de la organización es obligatorio")
    @Size(max = 200)
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
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
}

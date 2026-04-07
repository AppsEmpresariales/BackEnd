package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de creación para TipoDocumentoEntity.
 *
 * CAMPOS OMITIDOS (los gestiona el service o JPA):
 * - id: autogenerado.
 * - active: se establece en true al crear (US-024).
 * - creadoEn: gestionado por JPA.
 *
 * US-024 / US-042
 */
@Data
public class TipoDocumentoCreateDTO {

    @NotBlank(message = "El nombre del tipo documental es obligatorio")
    @Size(max = 150)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    @NotNull(message = "El NIT de la organización es obligatorio")
    private Long organizacionNit;
}

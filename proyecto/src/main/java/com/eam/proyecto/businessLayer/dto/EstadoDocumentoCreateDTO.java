package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de creación para EstadoDocumentoEntity.
 *
 * CAMPOS OMITIDOS (los gestiona el service o JPA):
 * - id: autogenerado.
 *
 * REGLAS DE NEGOCIO (validadas en el service):
 * - Solo puede haber un estado con esInicial=true por organización.
 * - El nombre debe ser único dentro de la misma organización.
 *
 * US-041
 */
@Data
public class EstadoDocumentoCreateDTO {

    @NotBlank(message = "El nombre del estado es obligatorio")
    @Size(max = 100)
    private String nombre;

    @Size(max = 7, message = "El color debe ser un código hexadecimal, ej: #4CAF50")
    private String color;

    /** Si es null, se interpreta como false. */
    private Boolean esInicial;

    /** Si es null, se interpreta como false. */
    private Boolean esFinal;

    @NotNull(message = "El NIT de la organización es obligatorio")
    private Long organizacionNit;
}

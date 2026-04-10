package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de actualización para EstadoDocumentoEntity.
 * Todos los campos son opcionales (actualización parcial PATCH).
 *
 * CAMPOS NO ACTUALIZABLES (ignorados por el mapper):
 * - id, organizacion: inmutables.
 *
 * US-041
 */
@Data
public class EstadoDocumentoUpdateDTO {

    @Size(max = 100)
    private String nombre;

    @Size(max = 7)
    private String color;

    private Boolean esInicial;
    private Boolean esFinal;
}

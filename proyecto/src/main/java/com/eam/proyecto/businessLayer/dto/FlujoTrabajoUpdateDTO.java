package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de actualización para FlujoTrabajoEntity.
 * Todos los campos son opcionales (actualización parcial PATCH).
 *
 * CAMPOS NO ACTUALIZABLES (ignorados por el mapper):
 * - id, organizacion: inmutables.
 * - tipoDocumento: no se puede cambiar el tipo documental de un flujo existente.
 *
 * US-044
 */
@Data
public class FlujoTrabajoUpdateDTO {

    @Size(max = 200)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    /** Activar / desactivar el flujo. */
    private Boolean activo;
}

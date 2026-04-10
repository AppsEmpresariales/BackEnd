package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de actualización para TipoDocumentoEntity.
 * Todos los campos son opcionales (actualización parcial PATCH).
 *
 * CAMPOS NO ACTUALIZABLES (ignorados por el mapper):
 * - id, creadoEn, organizacion: inmutables.
 * - active: se gestiona mediante endpoints dedicados activar/desactivar (US-026).
 *
 * US-025 / US-042
 */
@Data
public class TipoDocumentoUpdateDTO {

    @Size(max = 150)
    private String nombre;

    @Size(max = 500)
    private String descripcion;
}

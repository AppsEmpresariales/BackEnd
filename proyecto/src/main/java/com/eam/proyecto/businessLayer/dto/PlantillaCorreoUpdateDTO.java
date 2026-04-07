package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de actualización para PlantillaCorreoEntity.
 * Todos los campos son opcionales (actualización parcial PATCH).
 *
 * CAMPOS NO ACTUALIZABLES (ignorados por el mapper):
 * - id, organizacion: inmutables.
 * - activo: se gestiona mediante endpoints dedicados activar/desactivar (US-040).
 * - tipoEvento: la plantilla no cambia el evento que cubre (requiere recreación).
 *
 * US-040 / US-043
 */
@Data
public class PlantillaCorreoUpdateDTO {

    @Size(max = 200)
    private String nombre;

    @Size(max = 300)
    private String asunto;

    private String cuerpo;
}

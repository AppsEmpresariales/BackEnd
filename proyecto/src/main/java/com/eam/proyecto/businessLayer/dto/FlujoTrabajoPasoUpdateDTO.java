package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de actualización para FlujoTrabajoPasoEntity.
 * Todos los campos son opcionales (actualización parcial PATCH).
 *
 * CAMPOS NO ACTUALIZABLES (ignorados por el mapper):
 * - id, flujoTrabajo: inmutables.
 *
 * US-032
 */
@Data
public class FlujoTrabajoPasoUpdateDTO {

    @Size(max = 200)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    @Min(value = 1)
    private Integer ordenPaso;

    /** Cambiar el rol requerido para ejecutar el paso (US-029). */
    private Long rolRequeridoId;

    /** Cambiar el estado objetivo del paso (US-030). */
    private Long objetivoEstadoId;
}

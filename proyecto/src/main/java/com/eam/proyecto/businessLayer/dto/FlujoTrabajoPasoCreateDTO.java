package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de creación para FlujoTrabajoPasoEntity.
 *
 * CAMPOS OMITIDOS:
 * - id: autogenerado.
 *
 * REGLAS DE NEGOCIO (validadas en el service):
 * - ordenPaso debe ser único dentro del mismo flujoTrabajo (US-031).
 *
 * US-028 / US-029 / US-032
 */
@Data
public class FlujoTrabajoPasoCreateDTO {

    @NotBlank(message = "El nombre del paso es obligatorio")
    @Size(max = 200)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    @NotNull(message = "El orden del paso es obligatorio")
    @Min(value = 1, message = "El orden debe ser mayor o igual a 1")
    private Integer ordenPaso;

    @NotNull(message = "El flujo de trabajo es obligatorio")
    private Long flujoTrabajoId;

    /** Rol que debe completar este paso (US-029). */
    @NotNull(message = "El rol requerido es obligatorio")
    private Long rolRequeridoId;

    /** Estado al que transiciona el documento al completar el paso (US-030). */
    @NotNull(message = "El estado objetivo es obligatorio")
    private Long objetivoEstadoId;
}

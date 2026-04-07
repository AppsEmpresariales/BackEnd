package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de creación para FlujoTrabajoEntity.
 *
 * CAMPOS OMITIDOS (los gestiona el service):
 * - id: autogenerado.
 * - activo: el service lo inicializa en true al crear.
 *
 * REGLA DE NEGOCIO (validada en el service):
 * - Solo puede existir un flujo activo por tipo documental por organización (US-032).
 *
 * US-028 / US-032
 */
@Data
public class FlujoTrabajoCreateDTO {

    @NotBlank(message = "El nombre del flujo es obligatorio")
    @Size(max = 200)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    @NotNull(message = "El NIT de la organización es obligatorio")
    private Long organizacionNit;

    @NotNull(message = "El tipo documental es obligatorio")
    private Long tipoDocumentoId;
}

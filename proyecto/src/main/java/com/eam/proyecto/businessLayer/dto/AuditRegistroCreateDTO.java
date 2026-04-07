package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de creación para AuditRegistroEntity (append-only).
 * Solo se usa para registrar; nunca se edita ni elimina.
 *
 * CAMPOS OMITIDOS (los gestiona el service):
 * - id: autogenerado.
 * - creadoEn: asignado con LocalDateTime.now() en el service.
 *
 * US-033 / US-035
 */
@Data
public class AuditRegistroCreateDTO {

    @NotNull(message = "El documento es obligatorio")
    private Long documentoId;

    @NotNull(message = "La cédula del usuario es obligatoria")
    private Long usuarioCedula;

    @NotBlank(message = "La acción es obligatoria")
    @Size(max = 100)
    private String accion;

    @Size(max = 500)
    private String descripcion;

    @Size(max = 100)
    private String estadoPrevio;

    @Size(max = 100)
    private String estadoNuevo;
}

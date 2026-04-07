package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO de creación para FlujoTrabajoTareaEntity.
 * Usado al asignar una tarea de revisión a un usuario dentro del flujo.
 *
 * CAMPOS OMITIDOS (los gestiona el service):
 * - id: autogenerado.
 * - estado: el service lo inicializa en PENDIENTE (US-029).
 * - creadoEn: asignado con LocalDateTime.now().
 * - completadoEn: null hasta que se complete la tarea.
 *
 * US-029
 */
@Data
public class FlujoTrabajoTareaCreateDTO {

    @NotNull(message = "El documento es obligatorio")
    private Long documentoId;

    @NotNull(message = "El paso del flujo es obligatorio")
    private Long pasoId;

    @NotNull(message = "La cédula del usuario asignado es obligatoria")
    private Long asignadoACedula;

    private String comentario;

    /** Fecha límite para completar la tarea (US-039 — alertas de vencimiento). */
    private LocalDateTime fechaLimite;
}

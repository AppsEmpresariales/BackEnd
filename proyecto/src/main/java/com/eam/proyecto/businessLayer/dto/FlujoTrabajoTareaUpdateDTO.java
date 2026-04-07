package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de actualización para FlujoTrabajoTareaEntity.
 * Usado para cambiar el estado de la tarea (completar / cancelar) y agregar comentario.
 *
 * CAMPOS NO ACTUALIZABLES (ignorados por el mapper):
 * - id, documento, paso, asignadoA: inmutables.
 * - creadoEn, completadoEn: gestionados por el service.
 *
 * US-030
 */
@Data
public class FlujoTrabajoTareaUpdateDTO {

    /**
     * Nuevo estado: PENDIENTE, COMPLETADO, CANCELADO.
     * Validación de transición permitida en el service.
     */
    @Pattern(regexp = "PENDIENTE|COMPLETADO|CANCELADO",
             message = "Estado inválido. Valores permitidos: PENDIENTE, COMPLETADO, CANCELADO")
    private String estado;

    @Size(max = 1000)
    private String comentario;
}

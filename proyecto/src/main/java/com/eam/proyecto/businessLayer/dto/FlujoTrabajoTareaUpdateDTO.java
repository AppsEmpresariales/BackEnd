package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

/** US-030 */
@Data
public class FlujoTrabajoTareaUpdateDTO {

    @Pattern(regexp = "PENDIENTE|COMPLETADO|CANCELADO",
            message = "Estado inválido. Valores permitidos: PENDIENTE, COMPLETADO, CANCELADO")
    private String estado;

    @Size(max = 1000)
    private String comentario;

    private LocalDateTime fechaLimite;
}

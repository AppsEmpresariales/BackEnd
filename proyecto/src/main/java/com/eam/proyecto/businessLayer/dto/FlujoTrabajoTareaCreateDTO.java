package com.eam.proyecto.businessLayer.dto;

import com.eam.proyecto.persistenceLayer.entity.enums.EstadoTareaEnum;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

/** US-029 */
@Data
public class FlujoTrabajoTareaCreateDTO {

    @NotNull(message = "El documento es obligatorio")
    private Long documentoId;

    @NotNull(message = "El paso del flujo es obligatorio")
    private Long pasoId;

    @NotNull(message = "La cédula del usuario asignado es obligatoria")
    private Long asignadoACedula;

    private String comentario;
    private LocalDateTime fechaLimite;

    // Gestionados por el service antes de persistir
    private EstadoTareaEnum estado;
    private LocalDateTime creadoEn;
}

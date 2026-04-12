package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** US-024 / US-042 */
@Data
public class TipoDocumentoCreateDTO {

    @NotBlank(message = "El nombre del tipo documental es obligatorio")
    @Size(max = 150)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    @NotNull(message = "El NIT de la organización es obligatorio")
    private Long organizacionNit;

    // Gestionado por el service antes de persistir
    private Boolean active;
}

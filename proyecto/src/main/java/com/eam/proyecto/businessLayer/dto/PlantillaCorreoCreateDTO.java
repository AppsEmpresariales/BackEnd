package com.eam.proyecto.businessLayer.dto;

import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import jakarta.validation.constraints.*;
import lombok.Data;

/** US-040 / US-043 */
@Data
public class PlantillaCorreoCreateDTO {

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 200)
    private String nombre;

    @NotBlank(message = "El asunto del correo es obligatorio")
    @Size(max = 300)
    private String asunto;

    @NotBlank(message = "El cuerpo de la plantilla es obligatorio")
    private String cuerpo;

    @NotNull(message = "El tipo de evento es obligatorio")
    private TipoEventoEnum tipoEvento;

    @NotNull(message = "El NIT de la organización es obligatorio")
    private Long organizacionNit;

    // Gestionado por el service antes de persistir
    private Boolean activo;
}

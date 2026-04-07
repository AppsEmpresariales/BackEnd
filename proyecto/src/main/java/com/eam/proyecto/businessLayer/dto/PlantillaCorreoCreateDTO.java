package com.docucloud.businessLayer.dto;

import com.docucloud.persistence.enums.TipoEventoEnum;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de creación para PlantillaCorreoEntity.
 *
 * CAMPOS OMITIDOS (los gestiona el service):
 * - id: autogenerado.
 * - activo: se inicializa en true al crear (US-040).
 *
 * REGLA DE NEGOCIO (validada en el service):
 * - Solo puede existir una plantilla activa por tipoEvento por organización (US-043).
 *
 * US-040 / US-043
 */
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
}

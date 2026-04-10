package com.eam.proyecto.businessLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para EstadoDocumento.
 *
 * Representa un estado del ciclo de vida documental dentro de una organización.
 * Solo puede existir un estado con esInicial=true por organización (RF31).
 *
 * RF30 / RF31 / RF41
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estado del ciclo de vida de un documento dentro de la organización")
public class EstadoDocumentoDTO {

    @Schema(description = "ID único del estado", example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Nombre descriptivo del estado", example = "En Revisión",
            required = true, maxLength = 100)
    private String nombre;

    @Schema(description = "Color representativo del estado en formato hexadecimal",
            example = "#4CAF50", maxLength = 7)
    private String color;

    @Schema(description = "Indica si este es el estado inicial al crear un documento (único por organización)",
            example = "false")
    private Boolean esInicial;

    @Schema(description = "Indica si este es un estado final (el documento no avanza más)",
            example = "false")
    private Boolean esFinal;

    @Schema(description = "NIT de la organización propietaria de este estado",
            example = "900123456")
    private Long organizacionNit;
}

package com.eam.proyecto.businessLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para FlujoTrabajo.
 *
 * Representa un flujo de aprobación configurado para un tipo documental.
 * Solo puede existir un flujo activo por tipo documental por organización (RF32).
 * La organización y el tipo documental son inmutables una vez creado.
 *
 * RF28 / RF31 / RF32 / RF44
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Flujo de aprobación configurado para un tipo documental dentro de la organización")
public class FlujoTrabajoDTO {

    @Schema(description = "ID único del flujo de trabajo", example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Nombre descriptivo del flujo de trabajo",
            example = "Flujo Aprobación Contratos", required = true, maxLength = 200)
    private String nombre;

    @Schema(description = "Descripción del propósito y etapas del flujo",
            example = "Flujo de dos niveles: revisión jurídica y aprobación gerencial", maxLength = 500)
    private String descripcion;

    @Schema(description = "Indica si el flujo está activo y aplicable a nuevos documentos",
            example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean activo;

    @Schema(description = "NIT de la organización propietaria del flujo",
            example = "900123456")
    private Long organizacionNit;

    @Schema(description = "ID del tipo documental al que aplica este flujo",
            example = "3", required = true)
    private Long tipoDocumentoId;
}

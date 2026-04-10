package com.eam.proyecto.businessLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para TipoDocumento.
 *
 * Representa una categoría documental definida por la organización.
 * El nombre debe ser único dentro del mismo tenant (RF10).
 * Se desactiva lógicamente para preservar integridad referencial (RF26).
 *
 * RF24 / RF25 / RF26 / RF27 / RF42
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tipo documental definido por la organización para clasificar sus documentos")
public class TipoDocumentoDTO {

    @Schema(description = "ID único del tipo documental", example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Nombre del tipo documental (único por organización)",
            example = "Contrato", required = true, maxLength = 150)
    private String nombre;

    @Schema(description = "Descripción del propósito de este tipo documental",
            example = "Documentos contractuales con proveedores y clientes", maxLength = 500)
    private String descripcion;

    @Schema(description = "Indica si el tipo documental está activo y disponible para nuevos documentos",
            example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean active;

    @Schema(description = "NIT de la organización propietaria de este tipo documental",
            example = "900123456")
    private Long organizacionNit;

    @Schema(description = "Fecha y hora de creación del tipo documental",
            example = "2026-04-01T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime creadoEn;
}

package com.eam.proyecto.businessLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para Organización (tenant).
 *
 * Se usa cuando DEVOLVEMOS información de la organización al cliente.
 * NO expone contraseñas ni datos internos de seguridad.
 *
 * RF01 / RF08 / RF10 / RF11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información de la organización registrada en la plataforma")
public class OrganizacionDTO {

    @Schema(description = "NIT único de la organización", example = "900123456",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long nit;

    @Schema(description = "Nombre legal de la organización", example = "Empresa Demo S.A.",
            required = true, maxLength = 200)
    private String nombre;

    @Schema(description = "Correo electrónico de contacto de la organización",
            example = "admin@empresa.com", required = true, maxLength = 150)
    private String email;

    @Schema(description = "Número de teléfono de la organización",
            example = "+57-601-3456789", maxLength = 20)
    private String telefono;

    @Schema(description = "Nombre de la calle de la dirección",
            example = "Carrera 15", maxLength = 100)
    private String dirCalle;

    @Schema(description = "Número de la dirección",
            example = "#23-45", maxLength = 20)
    private String dirNumero;

    @Schema(description = "Comuna o barrio de la dirección",
            example = "Chapinero", maxLength = 100)
    private String dirComuna;

    @Schema(description = "Indica si la organización está activa en la plataforma",
            example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean active;

    @Schema(description = "Fecha y hora de registro de la organización",
            example = "2026-04-01T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime creadoEn;
}

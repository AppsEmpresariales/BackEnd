package com.eam.proyecto.businessLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para Usuario.
 *
 * Se usa cuando DEVOLVEMOS información del usuario al cliente.
 * NUNCA expone passwordHash ni datos de seguridad internos.
 *
 * RF12 / RF13 / RF14 / RF16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información del usuario registrado en la plataforma")
public class UsuarioDTO {

    @Schema(description = "Número de cédula único del usuario", example = "1234567890",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long cedula;

    @Schema(description = "Nombre completo del usuario", example = "Carlos Andrés Pérez",
            required = true, maxLength = 150)
    private String nombre;

    @Schema(description = "Correo electrónico del usuario (único por organización)",
            example = "carlos.perez@empresa.com", required = true, maxLength = 150)
    private String email;

    @Schema(description = "Indica si la cuenta del usuario está activa",
            example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean active;

    @Schema(description = "NIT de la organización a la que pertenece el usuario",
            example = "900123456", required = true)
    private Long organizacionNit;

    @Schema(description = "Fecha y hora de creación del usuario",
            example = "2026-04-01T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime creadoEn;

    @Schema(description = "Fecha y hora de la última actualización del usuario",
            example = "2026-04-05T15:45:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime actualizadoEn;
}

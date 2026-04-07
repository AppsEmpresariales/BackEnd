package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de actualización para UsuarioEntity.
 * Todos los campos son opcionales (actualización parcial PATCH).
 *
 * CAMPOS NO ACTUALIZABLES (ignorados por el mapper):
 * - cedula: PK natural inmutable.
 * - organizacion: el usuario no cambia de tenant.
 * - passwordHash: se actualiza por endpoint dedicado de cambio de contraseña.
 * - creadoEn / actualizadoEn: gestionados por JPA.
 *
 * US-013 / US-014
 */
@Data
public class UsuarioUpdateDTO {

    @Size(max = 150)
    private String nombre;

    @Email(message = "Formato de correo inválido")
    @Size(max = 150)
    private String email;

    /** Activar / inactivar la cuenta del usuario (US-014). */
    private Boolean active;
}

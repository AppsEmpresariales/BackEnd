package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de creación para UsuarioEntity.
 * Usado por el ADMIN_ORG para crear usuarios dentro de su organización.
 *
 * CAMPOS OMITIDOS (los gestiona el service):
 * - active: se establece en true al crear (US-012).
 * - creadoEn / actualizadoEn: gestionados por JPA.
 * - passwordHash: el service encripta la contraseña con BCrypt antes de persistir.
 *
 * US-012 / US-009
 */
@Data
public class UsuarioCreateDTO {

    @NotNull(message = "La cédula es obligatoria")
    private Long cedula;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150)
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotNull(message = "El NIT de la organización es obligatorio")
    private Long organizacionNit;
}

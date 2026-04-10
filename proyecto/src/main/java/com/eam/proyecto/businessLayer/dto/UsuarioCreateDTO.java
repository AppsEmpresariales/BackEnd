package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** US-012 */
@Data
public class UsuarioCreateDTO {

    @NotNull
    private Long cedula;

    @NotBlank
    private String nombre;

    @NotBlank
    @Email
    private String email;

    /** Contraseña en texto plano — el service la encripta con BCrypt antes de persistir */
    @NotBlank
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotNull
    private Long organizacionNit;

    // Gestionados por el service antes de persistir
    private String passwordHash;
    private Boolean active;
}

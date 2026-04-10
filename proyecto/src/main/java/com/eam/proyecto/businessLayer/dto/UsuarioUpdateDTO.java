package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** US-013 / US-014 */
@Data
public class UsuarioUpdateDTO {

    @Size(max = 150)
    private String nombre;

    @Email(message = "Formato de correo inválido")
    @Size(max = 150)
    private String email;

    private Boolean active;

    /** Contraseña en texto plano — el service la encripta con BCrypt antes de persistir */
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    /** Hash resultante — asignado por el service, no por el cliente */
    private String passwordHash;
}

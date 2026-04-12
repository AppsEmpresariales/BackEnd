package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

/** US-001 / US-008 */
@Data
public class OrganizacionCreateDTO {

    @NotNull(message = "El NIT es obligatorio")
    private Long nit;

    @NotBlank(message = "El nombre de la organización es obligatorio")
    @Size(max = 200)
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String telefono;

    @Size(max = 200)
    private String dirCalle;

    @Size(max = 20)
    private String dirNumero;

    @Size(max = 100)
    private String dirComuna;

    // Gestionados por el service antes de persistir
    private Boolean active;
    private LocalDateTime creadoEn;
}

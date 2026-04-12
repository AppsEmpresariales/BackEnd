package com.eam.proyecto.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrganizacionDTO {

    private Long nit;
    private String nombre;
    private String email;
    private String telefono;
    private String dirCalle;
    private String dirNumero;
    private String dirComuna;
    private Boolean active;
    private LocalDateTime creadoEn;
}

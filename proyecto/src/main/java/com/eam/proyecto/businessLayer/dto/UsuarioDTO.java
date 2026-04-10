package com.eam.proyecto.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UsuarioDTO {

    private Long cedula;
    private String nombre;
    private String email;
    private Boolean active;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;
}

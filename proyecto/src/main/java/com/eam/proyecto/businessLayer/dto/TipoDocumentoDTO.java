package com.eam.proyecto.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TipoDocumentoDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean active;
    private LocalDateTime creadoEn;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;
}

package com.eam.proyecto.businessLayer.dto;

import lombok.Data;

@Data
public class EstadoDocumentoDTO {

    private Long id;
    private String nombre;
    private String color;
    private Boolean esInicial;
    private Boolean esFinal;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;
}

package com.eam.proyecto.businessLayer.dto;

import lombok.Data;

@Data
public class FlujoTrabajoDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean active;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;

    /** Denormalizado desde tipoDocumento.id */
    private Long tipoDocumentoId;

    /** Denormalizado desde tipoDocumento.nombre */
    private String tipoDocumentoNombre;
}

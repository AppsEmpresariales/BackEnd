package com.docucloud.businessLayer.dto;

import lombok.Data;

/**
 * DTO de lectura para FlujoTrabajoEntity.
 * Incluye información denormalizada de la organización y tipo documental.
 *
 * US-028 / US-032 / US-044
 */
@Data
public class FlujoTrabajoDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;

    /** Denormalizado desde tipoDocumento.id (US-032) */
    private Long tipoDocumentoId;

    /** Denormalizado desde tipoDocumento.nombre */
    private String tipoDocumentoNombre;
}

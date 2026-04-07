package com.docucloud.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO de lectura para TipoDocumentoEntity.
 * Incluye información denormalizada de la organización.
 *
 * US-024 / US-027 / US-042
 */
@Data
public class TipoDocumentoDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean active;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;

    private LocalDateTime creadoEn;
}

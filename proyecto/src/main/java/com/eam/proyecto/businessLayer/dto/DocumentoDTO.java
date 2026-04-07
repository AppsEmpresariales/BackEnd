package com.docucloud.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO de lectura para DocumentoEntity.
 * Expone todos los campos con información denormalizada de las 4 relaciones.
 *
 * US-017 / US-021 / US-022 / US-023
 */
@Data
public class DocumentoDTO {

    private Long id;
    private String titulo;
    private String descripcion;
    private Integer version;

    /** Nombre del archivo tal como fue subido (US-018). */
    private String archivoNombre;

    /** Ruta interna / URL del archivo en storage (US-023). */
    private String archivoRuta;

    private Long tamanioArchivo;

    /** Denormalizado desde creadoPor.cedula */
    private Long creadoPorCedula;

    /** Denormalizado desde creadoPor.nombre */
    private String creadoPorNombre;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;

    /** Denormalizado desde tipoDocumento.id (US-027) */
    private Long tipoDocumentoId;

    /** Denormalizado desde tipoDocumento.nombre */
    private String tipoDocumentoNombre;

    /** Denormalizado desde estadoDocumento.id (US-030) */
    private Long estadoDocumentoId;

    /** Denormalizado desde estadoDocumento.nombre */
    private String estadoDocumentoNombre;

    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}

package com.eam.proyecto.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentoDTO {

    private Long id;
    private String titulo;
    private String descripcion;
    private Integer version;
    private String archivoNombre;
    private String archivoRuta;
    private Long tamanioArchivo;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    /** Denormalizado desde creadoPor.cedula */
    private Long creadoPorCedula;

    /** Denormalizado desde creadoPor.nombre */
    private String creadoPorNombre;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;

    /** Denormalizado desde tipoDocumento.id */
    private Long tipoDocumentoId;

    /** Denormalizado desde tipoDocumento.nombre */
    private String tipoDocumentoNombre;

    /** Denormalizado desde estadoDocumento.id */
    private Long estadoDocumentoId;

    /** Denormalizado desde estadoDocumento.nombre */
    private String estadoDocumentoNombre;
}

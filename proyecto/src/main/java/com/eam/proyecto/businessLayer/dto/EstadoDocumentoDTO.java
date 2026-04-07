package com.docucloud.businessLayer.dto;

import lombok.Data;

/**
 * DTO de lectura para EstadoDocumentoEntity.
 * Catálogo real en BD (no enum de Java). Cada organización define sus propios estados.
 *
 * US-030 / US-031 / US-041
 */
@Data
public class EstadoDocumentoDTO {

    private Long id;
    private String nombre;

    /** Color hexadecimal para representar el estado en UI (ej: "#4CAF50"). */
    private String color;

    /** true si es el estado asignado al crear un documento (US-017). */
    private Boolean esInicial;

    /** true si el documento no puede avanzar más en el flujo (US-031). */
    private Boolean esFinal;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;
}

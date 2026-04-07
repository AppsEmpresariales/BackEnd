package com.docucloud.businessLayer.dto;

import lombok.Data;

/**
 * DTO de lectura para PlantillaCorreoEntity.
 * Incluye información denormalizada de la organización.
 *
 * US-040 / US-043
 */
@Data
public class PlantillaCorreoDTO {

    private Long id;
    private String nombre;
    private String asunto;
    private String cuerpo;

    /**
     * Evento del sistema al que responde esta plantilla.
     * Valores: DOCUMENTO_CREADO, TAREA_ASIGNADA, TAREA_VENCIDA,
     *          DOCUMENTO_APROBADO, DOCUMENTO_RECHAZADO, NOTIFICACION_GENERAL.
     */
    private String tipoEvento;

    private Boolean activo;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;
}

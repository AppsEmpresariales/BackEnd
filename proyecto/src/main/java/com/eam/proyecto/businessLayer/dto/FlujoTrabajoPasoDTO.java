package com.docucloud.businessLayer.dto;

import lombok.Data;

/**
 * DTO de lectura para FlujoTrabajoPasoEntity.
 * Expone información denormalizada de las 3 relaciones del paso.
 *
 * US-028 / US-029 / US-031 / US-032
 */
@Data
public class FlujoTrabajoPasoDTO {

    private Long id;
    private String nombre;
    private String descripcion;

    /** Posición en la secuencia del flujo (US-031). */
    private Integer ordenPaso;

    /** Denormalizado desde flujoTrabajo.id */
    private Long flujoTrabajoId;

    /** Denormalizado desde flujoTrabajo.nombre */
    private String flujoTrabajoNombre;

    /** Denormalizado desde rolRequerido.id — rol que ejecuta el paso (US-029). */
    private Long rolRequeridoId;

    /** Denormalizado desde rolRequerido.nombre */
    private String rolRequeridoNombre;

    /** Denormalizado desde objetivoEstado.id — estado al que lleva completar el paso (US-030). */
    private Long objetivoEstadoId;

    /** Denormalizado desde objetivoEstado.nombre */
    private String objetivoEstadoNombre;
}

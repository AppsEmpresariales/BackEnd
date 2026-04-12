package com.eam.proyecto.businessLayer.dto;

import lombok.Data;

@Data
public class FlujoTrabajoPasoDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Integer ordenPaso;

    /** Denormalizado desde flujoTrabajo.id */
    private Long flujoTrabajoId;

    /** Denormalizado desde flujoTrabajo.nombre */
    private String flujoTrabajoNombre;

    /** Denormalizado desde rolRequerido.id */
    private Long rolRequeridoId;

    /** Denormalizado desde rolRequerido.nombre */
    private String rolRequeridoNombre;

    /** Denormalizado desde objetivoEstado.id */
    private Long objetivoEstadoId;

    /** Denormalizado desde objetivoEstado.nombre */
    private String objetivoEstadoNombre;
}

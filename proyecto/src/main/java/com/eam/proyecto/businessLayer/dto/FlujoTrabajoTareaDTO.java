package com.docucloud.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO de lectura para FlujoTrabajoTareaEntity.
 * Expone información denormalizada de las 3 relaciones y el enum de estado.
 *
 * US-029 / US-030 / US-039
 */
@Data
public class FlujoTrabajoTareaDTO {

    private Long id;

    /** Estado de la tarea: PENDIENTE, COMPLETADO, CANCELADO. */
    private String estado;

    private String comentario;
    private LocalDateTime fechaLimite;
    private LocalDateTime creadoEn;
    private LocalDateTime completadoEn;

    /** Denormalizado desde documento.id */
    private Long documentoId;

    /** Denormalizado desde documento.titulo */
    private String documentoTitulo;

    /** Denormalizado desde paso.id (US-031) */
    private Long pasoId;

    /** Denormalizado desde paso.nombre */
    private String pasoNombre;

    /** Denormalizado desde asignadoA.cedula (US-039) */
    private Long asignadoACedula;

    /** Denormalizado desde asignadoA.nombre */
    private String asignadoANombre;
}

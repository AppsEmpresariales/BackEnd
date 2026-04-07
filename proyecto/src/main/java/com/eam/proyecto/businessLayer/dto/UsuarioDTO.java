package com.docucloud.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO de lectura para UsuarioEntity.
 * Incluye información denormalizada de la organización.
 *
 * CAMPO OMITIDO: passwordHash — nunca se expone en respuestas.
 *
 * US-016 / US-002
 */
@Data
public class UsuarioDTO {

    private Long cedula;
    private String nombre;
    private String email;
    private Boolean active;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;

    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}

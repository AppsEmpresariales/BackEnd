package com.docucloud.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO de lectura para NotificacionEntity.
 * Expone todos los campos con información denormalizada de las 3 relaciones.
 *
 * US-037 / US-038 / US-039
 */
@Data
public class NotificacionDTO {

    private Long id;

    /** Canal de envío: EMAIL o SISTEMA. */
    private String canal;

    private String mensaje;
    private Boolean estaLeida;
    private LocalDateTime enviadaA;

    /** Denormalizado desde usuario.cedula */
    private Long usuarioCedula;

    /** Denormalizado desde usuario.nombre */
    private String usuarioNombre;

    /** Denormalizado desde documento.id */
    private Long documentoId;

    /** Denormalizado desde documento.titulo */
    private String documentoTitulo;

    /** Denormalizado desde plantilla.id (US-040) */
    private Long plantillaId;

    /** Denormalizado desde plantilla.nombre */
    private String plantillaNombre;
}

package com.docucloud.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO de lectura para AuditRegistroEntity.
 * Expone todos los campos del registro con información denormalizada.
 *
 * US-033 / US-034 / US-035 / US-036
 */
@Data
public class AuditRegistroDTO {

    private Long id;

    /** Tipo de acción realizada (ej: DOCUMENTO_CREADO, ESTADO_CAMBIADO, ARCHIVO_SUBIDO). */
    private String accion;

    private String descripcion;

    /** Estado del documento antes de la acción (US-035). */
    private String estadoPrevio;

    /** Estado del documento después de la acción (US-035). */
    private String estadoNuevo;

    private LocalDateTime creadoEn;

    /** Denormalizado desde documento.id */
    private Long documentoId;

    /** Denormalizado desde documento.titulo */
    private String documentoTitulo;

    /** Denormalizado desde usuario.cedula (US-035) */
    private Long usuarioCedula;

    /** Denormalizado desde usuario.nombre */
    private String usuarioNombre;
}

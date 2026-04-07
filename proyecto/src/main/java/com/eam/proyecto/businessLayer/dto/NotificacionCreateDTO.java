package com.docucloud.businessLayer.dto;

import com.docucloud.persistence.enums.CanalNotificacionEnum;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de creación para NotificacionEntity.
 * Usado internamente por los services al disparar notificaciones.
 *
 * CAMPOS OMITIDOS (los gestiona el service):
 * - id: autogenerado.
 * - estaLeida: se inicializa en false.
 * - enviadaA: asignado con LocalDateTime.now() al momento del envío real.
 *
 * US-037 / US-038 / US-039
 */
@Data
public class NotificacionCreateDTO {

    @NotNull(message = "La cédula del destinatario es obligatoria")
    private Long usuarioCedula;

    @NotNull(message = "El documento es obligatorio")
    private Long documentoId;

    /** Plantilla usada para generar el mensaje (US-040). Puede ser null. */
    private Long plantillaId;

    @NotNull(message = "El canal de notificación es obligatorio")
    private CanalNotificacionEnum canal;

    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;
}

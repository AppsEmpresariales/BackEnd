package com.eam.proyecto.businessLayer.dto;

import com.eam.proyecto.persistenceLayer.entity.enums.CanalNotificacionEnum;
import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

/** US-037 / US-038 / US-039 */
@Data
public class NotificacionCreateDTO {

    @NotNull(message = "La cédula del destinatario es obligatoria")
    private Long usuarioCedula;

    /** Alias usado por el controller para logging */
    private Long destinatarioCedula;

    @NotNull(message = "El documento es obligatorio")
    private Long documentoId;

    private Long plantillaId;

    @NotNull(message = "El canal de notificación es obligatorio")
    private CanalNotificacionEnum canal;

    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;

    /** Asunto del correo — resuelto desde la plantilla activa (RF40) */
    private String asunto;

    /** Usado internamente para buscar la plantilla activa por evento (RF40) */
    private TipoEventoEnum tipoEvento;

    /** NIT de la organización — para resolver plantilla activa (RF40) */
    private Long organizacionNit;

    // Gestionados por el service antes de persistir
    private Boolean estaLeida;
    private LocalDateTime enviadaA;
}
